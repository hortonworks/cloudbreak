package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("StackStopRestartAction")
public class StackStopRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getByIdWithListsInTransaction(stackPayload.getStackId());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED, stack.getStatusReason());
        super.restart(flowId, flowChainId, event, payload);
    }
}
