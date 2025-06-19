package com.sequenceiq.freeipa.flow.freeipa.trust.finish.action;

import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_FINISHED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FinishTrustSetupFinishedAction")
public class FinishTrustSetupFinishedAction extends AbstractFinishTrustSetupAction<StackEvent> {

    @Inject
    private OperationService operationService;

    public FinishTrustSetupFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        stackUpdater().updateStackStatus(stack, DetailedStackStatus.TRUST_SETUP_FINISH_SUCCESSFUL, "Finish setting up cross-realm trust was successful");
        operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        sendEvent(context, new StackEvent(FINISH_TRUST_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
