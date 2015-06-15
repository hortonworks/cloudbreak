package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.domain.BillingStatus.BILLING_STARTED;
import static com.sequenceiq.cloudbreak.domain.BillingStatus.BILLING_STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.repository.SubnetRepository;
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
    private static final String UPDATED_SUBNETS = "updated";
    private static final String REMOVED_SUBNETS = "removed";

    @Inject
    private StackService stackService;
    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private TerminationService terminationService;
    @Inject
    private StackStartService stackStartService;
    @Inject
    private StackStopService stackStopService;
    @Inject
    private StackScalingService stackScalingService;
    @Inject
    private MetadataSetupService metadataSetupService;
    @Inject
    private UserDataBuilder userDataBuilder;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private ClusterBootstrapper clusterBootstrapper;
    @Inject
    private ConsulMetadataSetup consulMetadataSetup;
    @Inject
    private ProvisioningService provisioningService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private SubnetRepository subnetRepository;

    @Override
    public FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        try {
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS);
            fireEventAndLog(actualContext.getStackId(), context, "Bootstrapping cluster on the infrastructure", UPDATE_IN_PROGRESS);
            clusterBootstrapper.bootstrapCluster(actualContext);
        } catch (Exception e) {
            LOGGER.error("Error occurred while bootstrapping container orchestrator: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        try {
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS);
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            fireEventAndLog(actualContext.getStackId(), context, "Setting up metadata", UPDATE_IN_PROGRESS);
            consulMetadataSetup.setupConsulMetadata(stack.getId());
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        } catch (Exception e) {
            LOGGER.error("Exception during the consul metadata setup process.", e);
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            stackUpdater.updateStackStatus(actualContext.getStackId(), START_IN_PROGRESS, "Cluster infrastructure is now starting.");
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            context = stackStartService.start(actualContext);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster infrastructure started successfully.");
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BILLING_STARTED.name(), "Cluster infrastructure started successfully.");
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process.", e);
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            if (stackStopService.isStopPossible(actualContext)) {
                stackUpdater.updateStackStatus(actualContext.getStackId(), STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
                Stack stack = stackService.getById(actualContext.getStackId());
                MDCBuilder.buildMdcContext(stack);
                context = stackStopService.stop(actualContext);
                stackUpdater.updateStackStatus(stack.getId(), STOPPED, "Cluster infrastructure stopped successfully.");
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BILLING_STOPPED.name(), "Cluster infrastructure stopped successfully.");
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        try {
            stackUpdater.updateStackStatus(actualContext.getStackId(), DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);

            if (stack != null && stack.getCredential() != null) {
                terminationService.terminateStack(stack.getId(), actualContext.getCloudPlatform());
            }

            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_STOPPED.name(),
                    "Billing stopped, the cluster and its infrastructure have been terminated.");
            terminationService.finalizeTermination(stack.getId());
            clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
            stackUpdater.updateStackStatus(actualContext.getStackId(), DELETE_COMPLETED,
                    "The cluster and its infrastructure have successfully been terminated.");

        } catch (Exception e) {
            LOGGER.error("Exception during the stack termination process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext addInstances(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            String statusReason = String.format("Add %s new instance(s) to the infrastructure.", actualContext.getScalingAdjustment());
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS, statusReason);
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            Set<Resource> resources = stackScalingService.addInstances(stack.getId(), actualContext.getInstanceGroup(), actualContext.getScalingAdjustment());
            context = new StackScalingContext(stack.getId(), actualContext.getCloudPlatform(), actualContext.getScalingAdjustment(),
                    actualContext.getInstanceGroup(), resources, actualContext.getScalingType(), null);
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext extendMetadata(FlowContext context) throws CloudbreakException {
        StackScalingContext actualCont = (StackScalingContext) context;
        Stack stack = stackService.getById(actualCont.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(stackService.getById(stack.getId()));
        fireEventAndLog(actualCont.getStackId(), context, "Extending metadata with new instances.", UPDATE_IN_PROGRESS);
        Set<String> upscaleCandidateAddresses = metadataSetupService.setupNewMetadata(stack.getId(), actualCont.getResources(), actualCont.getInstanceGroup());
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        hostGroupAdjustmentJson.setWithStackUpdate(false);
        hostGroupAdjustmentJson.setScalingAdjustment(actualCont.getScalingAdjustment());
        if (stack.getCluster() != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), actualCont.getInstanceGroup());
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
        }
        return new StackScalingContext(stack.getId(), actualCont.getCloudPlatform(), actualCont.getScalingAdjustment(), actualCont.getInstanceGroup(),
                actualCont.getResources(), actualCont.getScalingType(), upscaleCandidateAddresses);
    }

    @Override
    public FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            fireEventAndLog(actualContext.getStackId(), context, "Bootstrapping new node(s).", UPDATE_IN_PROGRESS);
            clusterBootstrapper.bootstrapNewNodes(actualContext);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Bootstrapping has been finished successfully.");
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of munchausen setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            MDCBuilder.buildMdcContext(stack);
            consulMetadataSetup.setupNewConsulMetadata(stack.getId(), actualContext.getUpscaleCandidateAddresses());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(actualContext.getScalingAdjustment());
            if (cluster != null) {
                HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), actualContext.getInstanceGroup());
                hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            }
            context = new ClusterScalingContext(stack.getId(), actualContext.getCloudPlatform(),
                    hostGroupAdjustmentJson, actualContext.getUpscaleCandidateAddresses(), new ArrayList<HostMetadata>(), actualContext.getScalingType());
        } catch (Exception e) {
            LOGGER.error("Exception during the extend consul metadata phase: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            String statusMessage = "Removing '%s' instance(s) from the cluster infrastructure.";
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
            cloudbreakEventService
                    .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), String.format(statusMessage, actualContext.getScalingAdjustment()));
            stackScalingService.downscaleStack(stack.getId(), actualContext.getInstanceGroup(), actualContext.getScalingAdjustment());
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Downscale of the cluster infrastructure finished successfully.");
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext stopRequested(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop requested process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext provision(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        try {
            Date startDate = new Date();
            Stack stack = stackService.getById(actualContext.getStackId());

            stackUpdater.updateStackStatus(stack.getId(), CREATE_IN_PROGRESS, "Creating infrastructure");
            ProvisionComplete provisionResult = provisioningService.buildStack(actualContext.getCloudPlatform(), stack, actualContext.getSetupProperties());
            Date endDate = new Date();

            long seconds = (endDate.getTime() - startDate.getTime()) / DateUtils.MILLIS_PER_SECOND;
            fireEventAndLog(stack.getId(), context, String.format("The creation of infrastructure took %ss.", seconds), AVAILABLE);
            context = new ProvisioningContext.Builder()
                    .setDefaultParams(provisionResult.getStackId(), provisionResult.getCloudPlatform())
                    .setProvisionedResources(provisionResult.getResources())
                    .build();
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop requested process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isDeleteInProgress()) {
                String statusReason = "State of the cluster infrastructure has been synchronized.";
                if (Status.stopStatusesForUpdate().contains(stack.getStatus())) {
                    stackUpdater.updateStackStatus(stack.getId(), STOPPED, statusReason);
                } else if (Status.availableStatusesForUpdate().contains(stack.getStatus())) {
                    stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, statusReason);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the stack sync process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext actualContext = (UpdateAllowedSubnetsContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Updating allowed subnets.");
            MDCBuilder.buildMdcContext(stack);
            Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(stack.cloudPlatform());
            Map<String, Set<Subnet>> modifiedSubnets = getModifiedSubnetList(stack, actualContext.getAllowedSubnets());
            Set<Subnet> newSubnets = modifiedSubnets.get(UPDATED_SUBNETS);
            stack.setAllowedSubnets(newSubnets);
            cloudPlatformConnectors.get(stack.cloudPlatform())
                    .updateAllowedSubnets(stack, userdata.get(InstanceGroupType.GATEWAY), userdata.get(InstanceGroupType.CORE));
            subnetRepository.delete(modifiedSubnets.get(REMOVED_SUBNETS));
            subnetRepository.save(newSubnets);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Updating of allowed subnets successfully finished.");
        } catch (Exception e) {
            Stack stack = stackService.getById(actualContext.getStackId());
            String msg = String.format("Failed to update security constraints with allowed subnets: %s", stack.getAllowedSubnets());
            if (stack != null && stack.isStackInDeletionPhase()) {
                msg = String.format("Failed to update security constraints with allowed subnets: %s; stack is already in deletion phase.",
                        stack.getAllowedSubnets());
            }
            LOGGER.error(msg, e);
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext actualContext = (UpdateAllowedSubnetsContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("Stack update failed. %s", actualContext.getErrorReason()));
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        try {
            Long id = null;
            String errorReason = null;
            if (context instanceof StackScalingContext) {
                StackScalingContext actualContext = (StackScalingContext) context;
                id = actualContext.getStackId();
                errorReason = actualContext.getErrorReason();
            } else if (context instanceof ClusterScalingContext) {
                ClusterScalingContext actualContext = (ClusterScalingContext) context;
                id = actualContext.getStackId();
                errorReason = actualContext.getErrorReason();
            }
            if (id != null) {
                Stack stack = stackService.getById(id);
                MDCBuilder.buildMdcContext(stack);
                stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack update failed. " + errorReason);
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        final Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        try {
            fireEventAndLog(actualContext.getStackId(), context, "Creation of infrastructure failed.", UPDATE_IN_PROGRESS);
            if (!stack.isStackInDeletionPhase()) {
                final CloudPlatform cloudPlatform = actualContext.getCloudPlatform();
                if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                    LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
                } else {
                    stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
                    cloudPlatformConnectors.get(cloudPlatform).rollback(stack, stack.getResources());
                    cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BILLING_STOPPED.name(), "Stack creation failed.");
                }
                stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, actualContext.getErrorReason());
            }

            context = new ProvisioningContext.Builder().setDefaultParams(stack.getId(), stack.cloudPlatform()).build();
        } catch (Exception ex) {
            LOGGER.error("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, String.format("Rollback failed: %s", ex.getMessage()));
            throw new CloudbreakException(String.format("Stack rollback failed on {} stack: ", stack.getId(), ex));
        }
        return context;
    }

    @Override
    public FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), DELETE_FAILED, "Termination failed: " + actualContext.getErrorReason());
        return context;
    }

    @Override
    public FlowContext handleStatusUpdateFailure(FlowContext flowContext) throws CloudbreakException {
        StackStatusUpdateContext context = (StackStatusUpdateContext) flowContext;
        Stack stack = stackService.getById(context.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (context.isStart()) {
            stackUpdater.updateStackStatus(context.getStackId(), START_FAILED, "Start failed: " + context.getErrorReason());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
            }
        } else {
            stackUpdater.updateStackStatus(context.getStackId(), STOP_FAILED, "Stop failed: " + context.getErrorReason());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
            }
        }
        return context;
    }

    private Map<String, Set<Subnet>> getModifiedSubnetList(Stack stack, List<Subnet> subnetList) {
        Map<String, Set<Subnet>> result = new HashMap<>();
        Set<Subnet> removed = new HashSet<>();
        Set<Subnet> updated = new HashSet<>();
        for (Subnet subnet : stack.getAllowedSubnets()) {
            if (!subnet.isModifiable()) {
                updated.add(subnet);
                removeFromNewSubnetList(subnet, subnetList);
            } else {
                removed.add(subnet);
            }
        }
        for (Subnet subnet : subnetList) {
            updated.add(subnet);
        }
        result.put(UPDATED_SUBNETS, updated);
        result.put(REMOVED_SUBNETS, removed);
        return result;
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

    private void fireEventAndLog(Long stackId, FlowContext context, String eventMessage, Status eventType) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", eventMessage, context);
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType.name(), eventMessage);
    }
}
