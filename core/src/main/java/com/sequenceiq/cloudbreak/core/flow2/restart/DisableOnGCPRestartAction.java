package com.sequenceiq.cloudbreak.core.flow2.restart;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

@Component("DisableOnGCPRestartAction")
public class DisableOnGCPRestartAction extends FillInMemoryStateStoreRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private FlowLogService flowLogService;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Payload stackPayload = (Payload) payload;
        Stack stack = stackService.getByIdWithTransaction(stackPayload.getResourceId());
        if (stack.getPlatformVariant().equals(GCP)) {
            try {
                flowLogService.terminate(stackPayload.getResourceId(), flowParameters.getFlowId());
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        } else {
            restart(flowParameters, flowChainId, event, payload, stack);
        }
    }
}
