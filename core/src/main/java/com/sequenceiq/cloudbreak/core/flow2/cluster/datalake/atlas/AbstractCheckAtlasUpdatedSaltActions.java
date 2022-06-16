package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.atlas;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedSaltFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas.CheckAtlasUpdatedStackEvent;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractCheckAtlasUpdatedSaltActions<P extends CheckAtlasUpdatedStackEvent>
        extends AbstractAction<FlowState, FlowEvent, CheckAtlasUpdatedSaltContext, P> {
    protected AbstractCheckAtlasUpdatedSaltActions(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CheckAtlasUpdatedSaltContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
            P payload) {
        return new CheckAtlasUpdatedSaltContext(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CheckAtlasUpdatedSaltContext> flowContext, Exception exception) {
        return new CheckAtlasUpdatedSaltFailedEvent(payload.getResourceId(), exception);
    }
}
