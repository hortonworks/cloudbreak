package com.sequenceiq.redbeams.flow.redbeams.common;

import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractRedbeamsFailureAction<S extends FlowState, E extends FlowEvent>
        extends AbstractAction<S, E, CommonContext, RedbeamsFailureEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedbeamsFailureAction.class);

    protected AbstractRedbeamsFailureAction() {
        super(RedbeamsFailureEvent.class);
    }

    @PostConstruct
    @Override
    public void init() {
        super.init();
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, RedbeamsFailureEvent payload) {
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(RedbeamsFailureEvent payload, Optional<CommonContext> flowContext, Exception ex) {
        LOGGER.error("Unexpected error during flow failure handling", ex);
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }

}
