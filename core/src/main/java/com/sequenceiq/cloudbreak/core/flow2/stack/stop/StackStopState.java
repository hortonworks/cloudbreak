package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStopState implements FlowState<StackStopState, StackStopEvent> {
    INIT_STATE,
    STOP_FAILED_STATE,
    STOP_STATE,
    STOP_FINISHED_STATE,
    FINAL_STATE;

    private StackStopEvent failureEvent;

    @Override
    public Class<?> action() {
        return null;
    }

    @Override
    public StackStopState failureState() {
        return null;
    }

    @Override
    public StackStopEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public void setFailureEvent(StackStopEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
