package com.sequenceiq.cloudbreak.core.flow.service;

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

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.StackStartService;
import com.sequenceiq.cloudbreak.core.flow.StackStopService;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
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
    private StackRepository stackRepository;

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

    @Override
    public FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        try {
            final Stack stack = stackRepository.findOneWithLists(provisioningContext.getStackId());
            if (!stack.isStackInDeletionPhase()) {
                final CloudPlatform cloudPlatform = provisioningContext.getCloudPlatform();

                if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                    LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
                } else {
                    cloudPlatformConnectors.get(cloudPlatform).rollback(stack, stack.getResources());
                }
                stackUpdater.updateStackStatusReason(provisioningContext.getStackId(), provisioningContext.getErrorReason());
                fireCloudbreakEventIfNeeded(provisioningContext.getStackId(), stack);
            }
            return new ProvisioningContext.Builder()
                    .setDefaultParams(stack.getId(), stack.cloudPlatform())
                    .build();
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack rollback failed on {} stack: ", provisioningContext.getStackId()), ex);
            throw new CloudbreakException(String.format("Stack rollback failed on {} stack: ", provisioningContext.getStackId(), ex));
        }
    }

    @Override
    public FlowContext start(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting stack. Context: {}", context);
        try {
            context = stackStartService.start(context);
            LOGGER.debug("Starting stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping stack. Context: {}", context);
        try {
            context = stackStopService.stop(context);
            LOGGER.debug("Stopping stack is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Terminating stack. Context: {}", context);
        try {
            DefaultFlowContext defaultFlowContext = (DefaultFlowContext) context;
            terminationService.terminateStack(defaultFlowContext.getStackId(), defaultFlowContext.getCloudPlatform());
            LOGGER.debug("Terminating stack is DONE");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the terminating process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException {
        DefaultFlowContext defaultFlowContext = (DefaultFlowContext) context;
        terminationService.handleTerminationFailure(defaultFlowContext.getStackId(), defaultFlowContext.getErrorReason());
        return context;
    }

    @Override
    public FlowContext handleStatusUpdateFailure(FlowContext flowContext) throws CloudbreakException {
        StackStatusUpdateContext context = (StackStatusUpdateContext) flowContext;
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
            stackScalingService.upscaleStack(updateContext.getStackId(), updateContext.getInstanceGroup(), updateContext.getScalingAdjustment());
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        try {
            StackScalingContext updateContext = (StackScalingContext) context;
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
            StackScalingContext updateContext = (StackScalingContext) context;
            stackUpdater.updateMetadataReady(updateContext.getStackId(), true);
            stackUpdater.updateStackStatus(updateContext.getStackId(), Status.AVAILABLE, "Stack update failed. " + updateContext.getErrorReason());
            return updateContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        try {
            UpdateAllowedSubnetsContext updateContext = (UpdateAllowedSubnetsContext) context;
            stackUpdater.updateMetadataReady(updateContext.getStackId(), true);
            stackUpdater.updateStackStatus(updateContext.getStackId(), Status.AVAILABLE, "Stack update failed. " + updateContext.getErrorReason());
            return updateContext;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnets failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext request = (UpdateAllowedSubnetsContext) context;
        Long stackId = request.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        String hostGroupUserData = userDataBuilder
                .buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(), new HashMap<String, String>(), InstanceGroupType.HOSTGROUP);
        String gateWayUserData = userDataBuilder
                .buildUserData(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(), new HashMap<String, String>(), InstanceGroupType.GATEWAY);
        try {
            stack.setAllowedSubnets(getNewSubnetList(stack, request.getAllowedSubnets()));
            cloudPlatformConnectors.get(stack.cloudPlatform()).updateAllowedSubnets(stack, gateWayUserData, hostGroupUserData);
            stackUpdater.updateStack(stack);
            String statusReason = "Security update successfully finished";
            stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusReason);
            return context;
        } catch (Exception e) {
            Stack tempStack = stackRepository.findById(stack.getId());
            String msg = String.format("Failed to update security constraints with allowed subnets: %s", stack.getAllowedSubnets());
            if (tempStack.isStackInDeletionPhase()) {
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

    private void fireCloudbreakEventIfNeeded(Long stackId, Stack stack) {
        if (stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
            cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STOPPED.name(), "Stack creation failed.");
        }
    }

}
