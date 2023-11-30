package com.sequenceiq.externalizedcompute.flow;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractExternalizedComputeClusterAction<P extends ExternalizedComputeClusterEvent>
        extends AbstractAction<FlowState, FlowEvent, ExternalizedComputeClusterContext, P> {

    protected AbstractExternalizedComputeClusterAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ExternalizedComputeClusterContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return ExternalizedComputeClusterContext.from(flowParameters, payload);
    }
}
