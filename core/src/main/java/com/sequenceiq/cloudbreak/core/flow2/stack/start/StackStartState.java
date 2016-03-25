package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStartState implements FlowState<StackStartState, StackStartEvent> {
    INIT_STATE(),
    START_FAILED_STATE(StackStartFailedAction.class),
    START_STATE(StackStartAction.class, StackStartEvent.START_FAILURE_EVENT),
    START_FINISHED_STATE(StackStartFinishedAction.class, StackStartEvent.START_FAILURE_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private StackStartEvent failureEvent;
    private StackStartState failureState;

    StackStartState() {
    }

    StackStartState(Class<?> action) {
        this.action = action;
    }

    StackStartState(Class<?> action, StackStartEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    StackStartState(Class<?> action, StackStartEvent failureEvent, StackStartState failureState) {
        this.action = action;
        this.failureEvent = failureEvent;
        this.failureState = failureState;
    }

    @Override
    public Class<?> action() {
        return action;
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
