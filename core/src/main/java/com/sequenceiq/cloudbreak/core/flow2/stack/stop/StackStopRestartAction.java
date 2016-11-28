package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("StackStopRestartAction")
public class StackStopRestartAction extends DefaultRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopRestartAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Stack stack = null;
        Payload stackPayload = (Payload) payload;
        try {
            stack = stackService.getById(stackPayload.getStackId());
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED, stack.getStatusReason());
        } catch (Exception e) {
            if (stack != null) {
                MDCBuilder.buildMdcContext(stack);
            }
            LOGGER.error("Failed to update status to STOP_REQUESTED", e);
        }
        super.restart(flowId, flowChainId, event, payload);
    }
}
