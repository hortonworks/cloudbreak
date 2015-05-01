package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.StackStartService;
import com.sequenceiq.cloudbreak.core.flow.StackStopService;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Service
public class SimpleStackFacade implements StackFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStackFacade.class);

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private StackService stackService;

    @Resource
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
    private UserDataBuilder userDataBuilder;

    @Autowired
    private HostGroupService hostGroupService;

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
                    stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS,
                            "Rollback is in progress, cause: " + provisioningContext.getErrorReason());
                    cloudPlatformConnectors.get(cloudPlatform).rollback(stack, stack.getResources());
                    cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_STOPPED.name(), "Stack creation failed.");
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
            MDCBuilder.buildMdcContext(stackService.getById(stackStatusUpdateContext.getStackId()));
            LOGGER.debug("Starting stack. Context: {}", stackStatusUpdateContext);
            context = stackStartService.start(stackStatusUpdateContext);
            LOGGER.debug("Starting stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process. Exception", e);
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(stackStatusUpdateContext.getStackId()));
            LOGGER.debug("Stopping stack. Context: {}", stackStatusUpdateContext);
            context = stackStopService.stop(stackStatusUpdateContext);
            LOGGER.debug("Stopping stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        try {
            DefaultFlowContext defaultFlowContext = (DefaultFlowContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(defaultFlowContext.getStackId()));
            LOGGER.debug("Terminating stack. Context: {}", context);
            terminationService.terminateStack(defaultFlowContext.getStackId(), defaultFlowContext.getCloudPlatform());
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
        terminationService.handleTerminationFailure(defaultFlowContext.getStackId(), defaultFlowContext.getErrorReason());
        return context;
    }

    @Override
    public FlowContext handleStatusUpdateFailure(FlowContext flowContext) throws CloudbreakException {
        StackStatusUpdateContext context = (StackStatusUpdateContext) flowContext;
        MDCBuilder.buildMdcContext(stackService.getById(context.getStackId()));
        LOGGER.info("Status update failure. Context: {}", context);
        FlowContext result;
        if (context.isStart()) {
            result = stackStartService.handleStackStartFailure(context);
        } else {
            result = stackStopService.handleStackStopFailure(context);
        }
        return result;
    }

    @Override
    public FlowContext upscaleStack(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext updateContext = (StackScalingContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
            LOGGER.info("Upscale stack. Context: {}", updateContext);

            Set<String> instanceIds = stackScalingService.upscaleStack(updateContext.getStackId(), updateContext.getInstanceGroup(),
                    updateContext.getScalingAdjustment());
            Stack stack = stackService.getById(updateContext.getStackId());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(updateContext.getScalingAdjustment());
            if (stack.getCluster() != null) {
                HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(stack.getCluster().getId(), updateContext.getInstanceGroup());
                hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            }
            UpdateAmbariHostsRequest updateAmbariHostsRequest = new UpdateAmbariHostsRequest(updateContext.getStackId(),
                    hostGroupAdjustmentJson,
                    instanceIds,
                    new ArrayList<HostMetadata>(),
                    false,
                    updateContext.getCloudPlatform(),
                    updateContext.getScalingType());
            return new ClusterScalingContext(updateAmbariHostsRequest);
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext updateContext = (StackScalingContext) context;
            MDCBuilder.buildMdcContext(stackService.getById(updateContext.getStackId()));
            LOGGER.info("Downscaling stack. Context: {}", updateContext);
            stackScalingService.downscaleStack(updateContext.getStackId(), updateContext.getInstanceGroup(), updateContext.getScalingAdjustment());
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
                StackScalingContext stackScalingContext =  (StackScalingContext) context;
                id = stackScalingContext.getStackId();
                errorReason = stackScalingContext.getErrorReason();
            } else if (context instanceof ClusterScalingContext) {
                ClusterScalingContext clusterScalingContext =  (ClusterScalingContext) context;
                id = clusterScalingContext.getStackId();
                errorReason = clusterScalingContext.getErrorReason();
            }
            if (id != null) {
                MDCBuilder.buildMdcContext(stackService.getById(id));
                LOGGER.info("Scaling failure. Context: {}", context);
                stackUpdater.updateMetadataReady(id, true);
                stackUpdater.updateStackStatus(id, Status.AVAILABLE, "Stack update failed. " + errorReason);
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
            stackUpdater.updateMetadataReady(updateContext.getStackId(), true);
            stackUpdater.updateStackStatus(updateContext.getStackId(), Status.AVAILABLE, "Stack update failed. " + updateContext.getErrorReason());
            return updateContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext request = (UpdateAllowedSubnetsContext) context;
        Long stackId = request.getStackId();
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            stack = stackService.getById(request.getStackId());
            MDCBuilder.buildMdcContext(stack);
            String hostGroupUserData = userDataBuilder.buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(),
                    new HashMap<String, String>(), InstanceGroupType.HOSTGROUP);
            String gateWayUserData = userDataBuilder.buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(),
                    new HashMap<String, String>(), InstanceGroupType.GATEWAY);
            stack.setAllowedSubnets(getNewSubnetList(stack, request.getAllowedSubnets()));
            cloudPlatformConnectors.get(stack.cloudPlatform()).updateAllowedSubnets(stack, gateWayUserData, hostGroupUserData);
            stackUpdater.updateStack(stack);
            stackUpdater.updateStackStatus(request.getStackId(), Status.AVAILABLE, "Security update successfully finished");
            return context;
        } catch (Exception e) {
            String msg = String.format("Failed to update security constraints with allowed subnets: %s", stack.getAllowedSubnets());
            if (stack != null && stack.isStackInDeletionPhase()) {
                msg = String.format("Failed to update security constraints with allowed subnets: %s, because stack is already in deletion phase.",
                        stack.getAllowedSubnets());
            }
            LOGGER.error(msg, e);
            throw new CloudbreakException(msg, e);
        }
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
