package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_FAILURE_HANDLED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FreeIpaTrustSetupFinishFailedAction")
public class FreeIpaTrustSetupFinishFailedAction extends FreeIpaTrustSetupFinishBaseAction<FreeIpaTrustSetupFinishFailureEvent> {

    @Inject
    private OperationService operationService;

    public FreeIpaTrustSetupFinishFailedAction() {
        super(FreeIpaTrustSetupFinishFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaTrustSetupFinishState,
            FreeIpaTrustSetupFinishFlowEvent> stateContext, FreeIpaTrustSetupFinishFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupFinishFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to finish cross-realm trust FreeIPA: " + getErrorReason(payload.getException());
        updateStatuses(stack, TRUST_SETUP_FINISH_FAILED, statusReason, TrustStatus.TRUST_SETUP_FINISH_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(TRUST_SETUP_FINISH_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
