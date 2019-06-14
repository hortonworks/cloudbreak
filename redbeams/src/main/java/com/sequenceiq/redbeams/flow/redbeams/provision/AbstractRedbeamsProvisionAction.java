package com.sequenceiq.redbeams.flow.redbeams.provision;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.statemachine.StateContext;

public abstract class AbstractRedbeamsProvisionAction<P extends Payload>
        extends AbstractAction<RedbeamsProvisionState, RedbeamsProvisionEvent, RedbeamsContext, P> {
    protected AbstractRedbeamsProvisionAction(Class<P> payloadClass) {
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
        StateContext<RedbeamsProvisionState, RedbeamsProvisionEvent> stateContext, P payload) {
        // Get the cloud context and the cloud credentials
        return new RedbeamsContext(flowParameters, null, null);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<RedbeamsContext> flowContext, Exception ex) {
        return new RedbeamsFailureEvent(payload.getResourceId(), ex);
    }
}
