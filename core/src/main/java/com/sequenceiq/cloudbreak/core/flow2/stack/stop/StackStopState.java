package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStopState implements FlowState<StackStopState, StackStopEvent> {
    INIT_STATE,
    STOP_FAILED_STATE,
    STOP_STATE(StackStopEvent.STOP_FAILURE_EVENT),
    STOP_FINISHED_STATE(StackStopEvent.STOP_FAILURE_EVENT),
    FINAL_STATE;

    private StackStopEvent failureEvent;
    private StackStopState failureState;

    StackStopState() {
    }

    StackStopState(StackStopEvent failureEvent) {
        this.failureEvent = failureEvent;
    }

    @Override
    public Class<?> action() {
        return null;
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
