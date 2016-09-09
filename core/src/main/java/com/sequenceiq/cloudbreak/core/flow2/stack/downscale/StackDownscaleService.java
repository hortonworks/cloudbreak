package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;

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

    public void startStackDownscale(StackScalingFlowContext context, Integer adjustment) {
        LOGGER.debug("Downscaling of stack ", context.getStack().getId());
        MDCBuilder.buildMdcContext(context.getStack());
        stackUpdater.updateStackStatus(context.getStack().getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name(), Math.abs(adjustment));
    }

    public void finishStackDownscale(StackScalingFlowContext context, String instanceGroupName, Set<String> instanceIds) {
        Stack stack = context.getStack();
        stackScalingService.updateRemovedResourcesState(stack, instanceIds, stack.getInstanceGroupByInstanceGroupName(instanceGroupName));
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Downscale of the cluster infrastructure finished successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name());

        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendDownScaleSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stack.getAmbariIp(), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_NOTIFICATION_EMAIL, AVAILABLE.name());
        }
    }

    public void handleStackDownscaleError(Exception errorDetails) {
        LOGGER.error("Exception during the downscaling of stack", errorDetails);
    }
}
