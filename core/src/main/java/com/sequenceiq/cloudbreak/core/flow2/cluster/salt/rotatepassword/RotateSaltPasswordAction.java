package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.rotatepassword;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordFailureResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class RotateSaltPasswordAction<P extends Payload>
        extends AbstractStackAction<RotateSaltPasswordState, RotateSaltPasswordEvent, RotateSaltPasswordContext, P> {

    public static final String REASON = "reason";

    public static final String TYPE = "type";

    private static final String PREVIOUS_STACK_STATUS = "previousStackStatus";

    @Inject
    private StackDtoService stackDtoService;

    protected RotateSaltPasswordAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected RotateSaltPasswordContext createFlowContext(FlowParameters flowParameters,
        StateContext<RotateSaltPasswordState, RotateSaltPasswordEvent> stateContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        StackStatus previousStackStatus = (StackStatus) variables.computeIfAbsent(PREVIOUS_STACK_STATUS, o -> stack.getStackStatus());
        RotateSaltPasswordReason reason = (RotateSaltPasswordReason) variables.getOrDefault(REASON, RotateSaltPasswordReason.MANUAL);
        RotateSaltPasswordType type = (RotateSaltPasswordType) variables.getOrDefault(TYPE, RotateSaltPasswordType.FALLBACK);
        return new RotateSaltPasswordContext(flowParameters, stack, previousStackStatus, reason, type);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RotateSaltPasswordContext> flowContext, Exception ex) {
        return new RotateSaltPasswordFailureResponse(payload.getResourceId(), ex);
    }
}
