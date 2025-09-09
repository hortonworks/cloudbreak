package com.sequenceiq.freeipa.flow;

import jakarta.inject.Inject;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;

public abstract class StackStatusFinalizerAbstractFlowConfig<S extends FlowState, E extends FlowEvent> extends AbstractFlowConfiguration<S, E> {

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    protected StackStatusFinalizerAbstractFlowConfig(Class<S> stateType, Class<E> eventType) {
        super(stateType, eventType);
    }

    @Override
    public final FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
