package com.sequenceiq.cloudbreak.core.flow2.restart;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
        Stack stack = stackService.getByIdWithListsInTransaction(stackPayload.getStackId());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.WAIT_FOR_SYNC, stack.getStatusReason());
        try {
            flowLogService.terminate(stackPayload.getStackId(), flowId);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }
}
