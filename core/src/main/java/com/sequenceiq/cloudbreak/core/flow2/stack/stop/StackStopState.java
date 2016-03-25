package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStopState implements FlowState<StackStopState, StackStopEvent> {
    INIT_STATE(),
    STOP_FAILED_STATE(StackStopFailedAction.class),
    STOP_STATE(StackStopAction.class, StackStopEvent.STOP_FAILURE_EVENT),
    STOP_FINISHED_STATE(StackStopFinishedAction.class, StackStopEvent.STOP_FAILURE_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private StackStopEvent failureEvent;
    private StackStopState failureState;

    StackStopState() {
    }

    StackStopState(Class<?> action) {
        this.action = action;
    }

    StackStopState(Class<?> action, StackStopEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    StackStopState(Class<?> action, StackStopEvent failureEvent, StackStopState failureState) {
        this.action = action;
        this.failureEvent = failureEvent;
        this.failureState = failureState;
    }

    @Override
    public Class<?> action() {
        return action;
    }

    @Override
    public StackStopState failureState() {
        return failureState;
    }

    @Override
    public StackStopEvent failureEvent() {
        return failureEvent;
    }

}
