package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStartState implements FlowState<StackStartState, StackStartEvent> {
    INIT_STATE,
    START_FAILED_STATE,
    START_STATE,
    START_FINISHED_STATE,
    FINAL_STATE;

    private StackStartEvent failureEvent;

    @Override
    public Class<?> action() {
        return null;
    }

    @Override
    public StackStartState failureState() {
        return null;
    }

    @Override
    public StackStartEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public void setFailureEvent(StackStartEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
