package com.sequenceiq.cloudbreak.core.flow2.restart;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("DisableOnGCPRestartAction")
public class DisableOnGCPRestartAction extends FillInMemoryStateStoreRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getByIdWithTransaction(stackPayload.getStackId());
        if (stack.getPlatformVariant().equals(GCP)) {
            try {
                flowLogService.terminate(stackPayload.getStackId(), flowId);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        } else {
            restart(flowId, flowChainId, event, payload, stack);
        }
    }
}
