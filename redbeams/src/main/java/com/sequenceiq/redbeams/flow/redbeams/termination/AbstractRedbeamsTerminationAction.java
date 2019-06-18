package com.sequenceiq.redbeams.flow.redbeams.termination;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.statemachine.StateContext;

public abstract class AbstractRedbeamsTerminationAction<P extends Payload>
        extends AbstractAction<RedbeamsTerminationState, RedbeamsTerminationEvent, RedbeamsContext, P> {
    protected AbstractRedbeamsTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected void doExecute(RedbeamsContext context, P payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context);
    }

    @Override
    protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
                                                StateContext<RedbeamsTerminationState, RedbeamsTerminationEvent> stateContext, P payload) {
        // Get the cloud context and the cloud credentials
        return new RedbeamsContext(flowParameters, null, null);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }
}
