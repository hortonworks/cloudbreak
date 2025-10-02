package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import static com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent.TRUST_CANCEL_FAILURE_HANDLED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FreeIpaTrustCancelFailedAction")
public class FreeIpaTrustCancelFailedAction extends FreeIpaTrustCancelAction<FreeIpaTrustCancelFailureEvent> {

    @Inject
    private OperationService operationService;

    public FreeIpaTrustCancelFailedAction() {
        super(FreeIpaTrustCancelFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaTrustCancelState,
            FreeIpaTrustCancelFlowEvent> stateContext, FreeIpaTrustCancelFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustCancelFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to cancel cross-realm trust on FreeIPA: " + getErrorReason(payload.getException());
        updateStatuses(stack, DetailedStackStatus.CANCEL_TRUST_SETUP_FAILED, statusReason, TrustStatus.CANCEL_TRUST_SETUP_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(TRUST_CANCEL_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }
}
