package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class StackDownscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackDownscaleService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private UsageService usageService;

    @Inject
    private StackUtil stackUtil;

    public void startStackDownscale(StackScalingFlowContext context, StackDownscaleTriggerEvent stackDownscaleTriggerEvent) {
        LOGGER.debug("Downscaling of stack ", context.getStack().getId());
        stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS);
        Set<String> hostNames = stackDownscaleTriggerEvent.getHostNames();
        Object msgParam = hostNames == null || hostNames.isEmpty() ? Math.abs(stackDownscaleTriggerEvent.getAdjustment()) : hostNames;
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name(), msgParam);
    }

    public void finishStackDownscale(StackScalingFlowContext context, String instanceGroupName, Set<String> instanceIds) {
        Stack stack = context.getStack();
        InstanceGroup g = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        int nodeCount = stackScalingService.updateRemovedResourcesState(stack, instanceIds, g);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED,
                "Downscale of the cluster infrastructure finished successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name());

        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendDownScaleSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stackUtil.extractAmbariIp(stack), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
        usageService.scaleUsagesForStack(stack.getId(), instanceGroupName, nodeCount);
    }

    public void handleStackDownscaleError(Exception errorDetails) {
        LOGGER.error("Exception during the downscaling of stack", errorDetails);
    }
}
