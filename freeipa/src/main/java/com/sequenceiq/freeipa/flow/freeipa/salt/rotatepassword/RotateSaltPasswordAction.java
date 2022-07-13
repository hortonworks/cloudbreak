package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class RotateSaltPasswordAction<P extends Payload>
        extends AbstractStackAction<RotateSaltPasswordState, RotateSaltPasswordEvent, RotateSaltPasswordContext, P> {

    public static final String REASON = "reason";

    @Inject
    private StackService stackService;

    protected RotateSaltPasswordAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RotateSaltPasswordContext createFlowContext(FlowParameters flowParameters,
            StateContext<RotateSaltPasswordState, RotateSaltPasswordEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        RotateSaltPasswordReason reason = (RotateSaltPasswordReason) variables.getOrDefault(REASON, RotateSaltPasswordReason.MANUAL);
        return new RotateSaltPasswordContext(flowParameters, stack, reason);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RotateSaltPasswordContext> flowContext, Exception ex) {
        return new RotateSaltPasswordFailureResponse(payload.getResourceId(), ex);
    }
}
