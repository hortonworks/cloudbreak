package com.sequenceiq.freeipa.flow.freeipa.binduser.create.action;

import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_FAILED_EVENT;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserState;

public abstract class AbstractBindUserCreateAction<P extends CreateBindUserEvent>
        extends AbstractAction<CreateBindUserState, CreateBindUserFlowEvent, CommonContext, P> {

    protected AbstractBindUserCreateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<CreateBindUserState, CreateBindUserFlowEvent> stateContext,
            P payload) {
        MDCBuilder.addOperationId(payload.getOperationId());
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        return new CreateBindUserFailureEvent(CREATE_BIND_USER_FAILED_EVENT.event(), payload, "Unexpected failure", ex);
    }
}
