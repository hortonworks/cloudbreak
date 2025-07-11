package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("TrustSetupFailedAction")
public class TrustSetupFailedAction extends AbstractTrustSetupAction<TrustSetupFailureEvent> {

    @Inject
    private OperationService operationService;

    protected TrustSetupFailedAction() {
        super(TrustSetupFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaTrustSetupState,
            FreeIpaTrustSetupFlowEvent> stateContext, TrustSetupFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, TrustSetupFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to prepare cross-realm trust FreeIPA: " + getErrorReason(payload.getException());
        updateStatuses(context.getStack(), DetailedStackStatus.TRUST_SETUP_FAILED, statusReason, TrustStatus.TRUST_SETUP_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason);
        sendEvent(context, new StackEvent(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_HANDLED_EVENT.event(),
                payload.getResourceId()));
    }
}
