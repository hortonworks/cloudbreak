package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_FINISHED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("CancelTrustSetupFinishedAction")
public class CancelTrustSetupFinishedAction extends AbstractCancelTrustSetupAction<StackEvent> {

    @Inject
    private OperationService operationService;

    public CancelTrustSetupFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        updateStatuses(stack, DetailedStackStatus.CANCEL_TRUST_SETUP_SUCCESSFUL, "Cancel cross-realm trust was successful",
                TrustStatus.TRUST_SETUP_REQUIRED);
        operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        sendEvent(context, new StackEvent(CANCEL_TRUST_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
