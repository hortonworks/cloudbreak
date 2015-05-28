package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.domain.BillingStatus.BILLING_STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulMetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Service
public class SimpleStackFacade implements StackFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStackFacade.class);

    @Autowired
    private StackService stackService;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    private TerminationService terminationService;

    @Autowired
    private StackStartService stackStartService;

    @Autowired
    private StackStopService stackStopService;

    @Autowired
    private StackScalingService stackScalingService;

    @Autowired
    private MetadataSetupService metadataSetupService;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private ClusterBootstrapper clusterBootstrapper;

    @Autowired
    private ConsulMetadataSetup consulMetadataSetup;

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException {
        try {
            clusterBootstrapper.bootstrapCluster((ProvisioningContext) context);
            return context;
        } catch (Exception e) {
            LOGGER.error("Error occurred while bootstrapping container orchestrator: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException {
        try {
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(provisioningContext.getStackId()));
            LOGGER.debug("Setting up consul metadata. Context: {}", context);
            consulMetadataSetup.setupConsulMetadata(provisioningContext.getStackId());
            stackUpdater.updateStackStatus(provisioningContext.getStackId(), AVAILABLE, "Stack is ready");
            return provisioningContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the consul metadata setup process.", e);
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        final Stack stack = stackService.getById(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Stack creation failure. Context: {}", provisioningContext);
        try {
            if (!stack.isStackInDeletionPhase()) {
                final CloudPlatform cloudPlatform = provisioningContext.getCloudPlatform();

                if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                    LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
                } else {
                    stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS,
                            "Rollback is in progress, cause: " + provisioningContext.getErrorReason());
                    cloudPlatformConnectors.get(cloudPlatform).rollback(stack, stack.getResources());
                    cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BILLING_STOPPED.name(), "Stack creation failed.");
                }
                stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_FAILED, provisioningContext.getErrorReason());
            }
            return new ProvisioningContext.Builder()
                    .setDefaultParams(stack.getId(), stack.cloudPlatform())
                    .build();
        } catch (Exception ex) {
            LOGGER.error("Stack rollback failed on stack id : {}. Exception:", provisioningContext.getStackId(), ex);
            stackUpdater.updateStackStatus(provisioningContext.getStackId(), Status.CREATE_FAILED, "Rollback failed: " + ex.getMessage());
            throw new CloudbreakException(String.format("Stack rollback failed on {} stack: ", provisioningContext.getStackId(), ex));
        }
    }

    @Override
    public FlowContext start(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), START_IN_PROGRESS, "Cluster infrastructure is now starting.");
            MDCBuilder.buildMdcContext(stackService.getById(stackStatusUpdateContext.getStackId()));
            LOGGER.debug("Starting stack. Context: {}", stackStatusUpdateContext);
            context = stackStartService.start(stackStatusUpdateContext);
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), AVAILABLE, "Instances were started.");
            LOGGER.debug("Starting stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process.", e);
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
            String stopRequestedMsg = "Stopping of cluster infrastructure has been requested.";
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), STOP_REQUESTED, stopRequestedMsg);
            MDCBuilder.buildMdcContext(stackService.getById(stackStatusUpdateContext.getStackId()));
            LOGGER.debug("Stopping stack. Context: {}", stackStatusUpdateContext);
            if (stackStopService.isStopPossible(stackStatusUpdateContext)) {
                stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), Status.STOP_IN_PROGRESS, "Cluster infrastructure is stopping.");
                context = stackStopService.stop(stackStatusUpdateContext);
                LOGGER.info("Update stack state to: {}", STOPPED);
                stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), STOPPED, "Cluster infrastructure stopped successfully.");
                cloudbreakEventService.fireCloudbreakEvent(stackStatusUpdateContext.getStackId(), BILLING_STOPPED.name(), "Cluster infrastructure stopped.");
                LOGGER.debug("Stopping stack is DONE.");
            }
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        try {
            DefaultFlowContext defaultFlowContext = (DefaultFlowContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(defaultFlowContext.getStackId()));
            LOGGER.debug("Terminating stack. Context: {}", context);
            stackUpdater.updateStackStatus(defaultFlowContext.getStackId(), DELETE_IN_PROGRESS, "Terminating cluster infrastructure.");
            terminationService.terminateStack(defaultFlowContext.getStackId(), defaultFlowContext.getCloudPlatform());
            terminationService.finalizeTermination(defaultFlowContext.getStackId());
            String deleteCompletedMessage = "The cluster and its infrastructure have successfully been terminated.";
            updateClusterStatus(defaultFlowContext.getStackId(), DELETE_COMPLETED, deleteCompletedMessage);
            cloudbreakEventService.fireCloudbreakEvent(defaultFlowContext.getStackId(), Status.DELETE_COMPLETED.name(), deleteCompletedMessage);
            String billingStoppedMessage = "Billing stopped; the cluster and its infrastructure have been terminated.";
            stackUpdater.updateStackStatus(defaultFlowContext.getStackId(), DELETE_COMPLETED, billingStoppedMessage);
            cloudbreakEventService.fireCloudbreakEvent(defaultFlowContext.getStackId(), BillingStatus.BILLING_STOPPED.name(), billingStoppedMessage);
            LOGGER.debug("Terminating stack is DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack termination process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException {
        DefaultFlowContext defaultFlowContext = (DefaultFlowContext) context;
        MDCBuilder.buildMdcContext(stackService.getById(defaultFlowContext.getStackId()));
        LOGGER.info("Termination failure. Context: {}", defaultFlowContext);
        LOGGER.info("Failed to delete stack {}. Setting it's status to {}.", defaultFlowContext.getStackId(), DELETE_FAILED);
        stackUpdater.updateStackStatus(defaultFlowContext.getStackId(), DELETE_FAILED, defaultFlowContext.getErrorReason());
        return context;
    }

    @Override
    public FlowContext handleStatusUpdateFailure(FlowContext flowContext) throws CloudbreakException {
        StackStatusUpdateContext context = (StackStatusUpdateContext) flowContext;
        MDCBuilder.buildMdcContext(stackService.getById(context.getStackId()));
        LOGGER.info("Status update failure. Context: {}", context);
        if (context.isStart()) {
            LOGGER.info("Update stack state to: {}", START_FAILED);
            stackUpdater.updateStackStatus(context.getStackId(), START_FAILED, context.getErrorReason());
        } else {
            LOGGER.info("Update stack state to: {}", STOP_FAILED);
            stackUpdater.updateStackStatus(context.getStackId(), STOP_FAILED, context.getErrorReason());
        }
        return context;
    }

    @Override
    public FlowContext addInstances(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext updateContext = (StackScalingContext) context;
            String statusMessage = "Adding '%s' new instance(s) to the cluster infrastructure.";
            stackUpdater.updateStackStatus(updateContext.getStackId(), UPDATE_IN_PROGRESS, String.format(statusMessage, updateContext.getScalingAdjustment()));
            MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
            Set<Resource> resources = stackScalingService.addInstances(updateContext.getStackId(), updateContext.getInstanceGroup(),
                    updateContext.getScalingAdjustment());
            return new StackScalingContext(updateContext.getStackId(),
                    updateContext.getCloudPlatform(),
                    updateContext.getScalingAdjustment(),
                    updateContext.getInstanceGroup(),
                    resources,
                    updateContext.getScalingType(),
                    null);

        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext extendMetadata(FlowContext context) throws CloudbreakException {
        StackScalingContext updateContext = (StackScalingContext) context;
        MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
        Set<String> upscaleCandidateAddresses = metadataSetupService.setupNewMetadata(
                updateContext.getStackId(),
                updateContext.getResources(),
                updateContext.getInstanceGroup());

        Stack stack = stackService.getById(updateContext.getStackId());
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        hostGroupAdjustmentJson.setWithStackUpdate(false);
        hostGroupAdjustmentJson.setScalingAdjustment(updateContext.getScalingAdjustment());
        if (stack.getCluster() != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(stack.getCluster().getId(), updateContext.getInstanceGroup());
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
        }
        return new StackScalingContext(
                updateContext.getStackId(),
                updateContext.getCloudPlatform(),
                updateContext.getScalingAdjustment(),
                updateContext.getInstanceGroup(),
                updateContext.getResources(),
                updateContext.getScalingType(),
                upscaleCandidateAddresses);
    }

    @Override
    public FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException {
        StackScalingContext scalingContext = (StackScalingContext) context;
        try {
            clusterBootstrapper.bootstrapNewNodes(scalingContext);
            stackUpdater.updateStackStatus(scalingContext.getStackId(), AVAILABLE, "");
            return scalingContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of munchausen setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext stackContext = (StackScalingContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(stackContext.getStackId()));
            consulMetadataSetup.setupNewConsulMetadata(stackContext.getStackId(), stackContext.getUpscaleCandidateAddresses());

            Stack stack = stackService.getById(stackContext.getStackId());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(stackContext.getScalingAdjustment());
            if (stack.getCluster() != null) {
                HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(stack.getCluster().getId(), stackContext.getInstanceGroup());
                hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            }
            return new ClusterScalingContext(
                    stackContext.getStackId(),
                    stackContext.getCloudPlatform(),
                    hostGroupAdjustmentJson,
                    stackContext.getUpscaleCandidateAddresses(),
                    new ArrayList<HostMetadata>(),
                    stackContext.getScalingType());
        } catch (Exception e) {
            LOGGER.error("Exception during the extend consul metadata phase: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext updateContext = (StackScalingContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
            LOGGER.info("Downscaling stack. Context: {}", updateContext);
            String statusMessage = "Removing '%s' instance(s) from the cluster infrastructure.";
            stackUpdater.updateStackStatus(updateContext.getStackId(), UPDATE_IN_PROGRESS, String.format(statusMessage, updateContext.getScalingAdjustment()));
            stackScalingService.downscaleStack(updateContext.getStackId(), updateContext.getInstanceGroup(), updateContext.getScalingAdjustment());
            stackUpdater.updateStackStatus(updateContext.getStackId(), AVAILABLE, "Downscaling of cluster infrastructure was successful.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        try {
            Long id = null;
            String errorReason = null;
            if (context instanceof StackScalingContext) {
                StackScalingContext stackScalingContext = (StackScalingContext) context;
                id = stackScalingContext.getStackId();
                errorReason = stackScalingContext.getErrorReason();
            } else if (context instanceof ClusterScalingContext) {
                ClusterScalingContext clusterScalingContext = (ClusterScalingContext) context;
                id = clusterScalingContext.getStackId();
                errorReason = clusterScalingContext.getErrorReason();
            }
            if (id != null) {
                MDCBuilder.buildMdcContext(stackService.getById(id));
                LOGGER.info("Scaling failure. Context: {}", context);
                stackUpdater.updateStackStatus(id, AVAILABLE, "Stack update failed. " + errorReason);
            }
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        try {
            UpdateAllowedSubnetsContext updateContext = (UpdateAllowedSubnetsContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
            LOGGER.info("Update allowed subnets failure. Context {}", updateContext);
            stackUpdater.updateStackStatus(updateContext.getStackId(), AVAILABLE, "Stack update failed. " + updateContext.getErrorReason());
            return updateContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stopRequested(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(stackStatusUpdateContext.getStackId()));
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), STOP_REQUESTED,
                    "Stopping of cluster infrastructure has been requested.");
            LOGGER.debug("Stop requested stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop requested process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext provision(FlowContext context) throws CloudbreakException {
        try {
            Date startDate = new Date();
            ProvisioningContext provisioningContext = (ProvisioningContext) context;
            stackUpdater.updateStackStatus(provisioningContext.getStackId(), CREATE_IN_PROGRESS,
                    "Creation of cluster infrastructure has started on the cloud provider.");
            Stack stack = stackService.getById(provisioningContext.getStackId());
            ProvisionComplete provisionResult = provisioningService.buildStack(provisioningContext.getCloudPlatform(), stack,
                provisioningContext.getSetupProperties());
            LOGGER.debug("Provisioning DONE.");
            Date endDate = new Date();
            long seconds = (endDate.getTime() - startDate.getTime()) / DateUtils.MILLIS_PER_SECOND;
            cloudbreakEventService.fireCloudbreakEvent(provisioningContext.getStackId(), AVAILABLE.name(),
                    String.format("The creation of the instratructure was %s sec", seconds));
            return new ProvisioningContext.Builder()
                    .setDefaultParams(provisionResult.getStackId(), provisionResult.getCloudPlatform())
                    .setProvisionedResources(provisionResult.getResources())
                    .build();
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop requested process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext request = (UpdateAllowedSubnetsContext) context;
        Long stackId = request.getStackId();
        stackUpdater.updateStackStatus(stackId, UPDATE_IN_PROGRESS, "Updating allowed subnets");
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            stack = stackService.getById(request.getStackId());
            MDCBuilder.buildMdcContext(stack);
            Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(stack.cloudPlatform());
            stack.setAllowedSubnets(getNewSubnetList(stack, request.getAllowedSubnets()));
            cloudPlatformConnectors.get(stack.cloudPlatform())
                    .updateAllowedSubnets(stack, userdata.get(InstanceGroupType.GATEWAY), userdata.get(InstanceGroupType.CORE));
            stackUpdater.updateStack(stack);
            stackUpdater.updateStackStatus(request.getStackId(), AVAILABLE, "Security update successfully finished");
            return context;
        } catch (Exception e) {
            String msg = String.format("Failed to update security constraints with allowed subnets: %s", stack.getAllowedSubnets());
            if (stack != null && stack.isStackInDeletionPhase()) {
                msg = String.format("Failed to update security constraints with allowed subnets: %s; stack is already in deletion phase.",
                        stack.getAllowedSubnets());
            }
            LOGGER.error(msg, e);
            throw new CloudbreakException(e);
        }
    }

    private void updateClusterStatus(Long stackId, Status status, String statusMessage) {
        Cluster cluster = clusterService.retrieveClusterByStackId(stackId);
        cluster.setStatusReason(statusMessage);
        cluster.setStatus(status);
        stackUpdater.updateStackCluster(stackId, cluster);
        cloudbreakEventService.fireCloudbreakEvent(stackId, status.name(), statusMessage);
    }

    private Set<Subnet> getNewSubnetList(Stack stack, List<Subnet> subnetList) {
        Set<Subnet> copy = new HashSet<>();
        for (Subnet subnet : stack.getAllowedSubnets()) {
            if (!subnet.isModifiable()) {
                copy.add(subnet);
                removeFromNewSubnetList(subnet, subnetList);
            }
        }
        for (Subnet subnet : subnetList) {
            copy.add(subnet);
        }
        return copy;
    }

    private void removeFromNewSubnetList(Subnet subnet, List<Subnet> subnetList) {
        Iterator<Subnet> iterator = subnetList.iterator();
        String cidr = subnet.getCidr();
        while (iterator.hasNext()) {
            Subnet next = iterator.next();
            if (next.getCidr().equals(cidr)) {
                iterator.remove();
            }
        }
    }
}
