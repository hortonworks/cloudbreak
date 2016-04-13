package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStartState implements FlowState<StackStartState, StackStartEvent> {
    INIT_STATE,
    START_FAILED_STATE,
    START_STATE(StackStartEvent.START_FAILURE_EVENT),
    START_FINISHED_STATE(StackStartEvent.START_FAILURE_EVENT),
    FINAL_STATE;

    private StackStartEvent failureEvent;
    private StackStartState failureState;

    StackStartState() {
    }

    StackStartState(StackStartEvent failureEvent) {
        this.failureEvent = failureEvent;
    }

    @Override
    public Class<?> action() {
        return null;
    }

    @Override
    public StackStartState failureState() {
        return failureState;
    }

    @Override
    public StackStartEvent failureEvent() {
        return failureEvent;
    }
}
