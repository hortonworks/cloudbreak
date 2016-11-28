package com.sequenceiq.cloudbreak.core.flow2.restart;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("WaitForSyncRestartAction")
public class WaitForSyncRestartAction extends DefaultRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitForSyncRestartAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Stack stack = null;
        Payload stackPayload = (Payload) payload;
        try {
            stack = stackService.getById(stackPayload.getStackId());
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.WAIT_FOR_SYNC, stack.getStatusReason());
        } catch (Exception e) {
            if (stack != null) {
                MDCBuilder.buildMdcContext(stack);
            }
            LOGGER.error("Failed to update status to WAIT_FOR_SYNC and terminate flow", e);
        }
        flowLogService.terminate(stackPayload.getStackId(), flowId);
    }
}
