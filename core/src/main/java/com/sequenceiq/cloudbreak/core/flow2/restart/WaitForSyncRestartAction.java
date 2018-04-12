package com.sequenceiq.cloudbreak.core.flow2.restart;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("WaitForSyncRestartAction")
public class WaitForSyncRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getByIdWithLists(stackPayload.getStackId());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.WAIT_FOR_SYNC, stack.getStatusReason());
        flowLogService.terminate(stackPayload.getStackId(), flowId);
    }
}
