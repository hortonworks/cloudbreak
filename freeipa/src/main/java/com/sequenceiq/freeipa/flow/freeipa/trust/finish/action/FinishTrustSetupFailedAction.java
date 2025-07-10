package com.sequenceiq.freeipa.flow.freeipa.trust.finish.action;

import static com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_FAILURE_HANDLED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FinishTrustSetupFailedAction")
public class FinishTrustSetupFailedAction extends AbstractFinishTrustSetupAction<FinishTrustSetupFailureEvent> {

    @Inject
    private OperationService operationService;

    public FinishTrustSetupFailedAction() {
        super(FinishTrustSetupFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaFinishTrustSetupState,
            FreeIpaFinishTrustSetupFlowEvent> stateContext, FinishTrustSetupFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, FinishTrustSetupFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to finish cross-realm trust FreeIPA: " + getErrorReason(payload.getException());
        updateStatuses(stack, DetailedStackStatus.TRUST_SETUP_FINISH_FAILED, statusReason, TrustStatus.TRUST_SETUP_FINISH_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(FINISH_TRUST_SETUP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
