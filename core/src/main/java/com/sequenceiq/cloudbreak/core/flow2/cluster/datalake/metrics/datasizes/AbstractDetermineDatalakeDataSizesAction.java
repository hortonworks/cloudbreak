package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractDetermineDatalakeDataSizesAction<P extends StackEvent>
        extends AbstractAction<FlowState, FlowEvent, DetermineDatalakeDataSizesContext, P> {
    protected AbstractDetermineDatalakeDataSizesAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected DetermineDatalakeDataSizesContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return new DetermineDatalakeDataSizesContext(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<DetermineDatalakeDataSizesContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
