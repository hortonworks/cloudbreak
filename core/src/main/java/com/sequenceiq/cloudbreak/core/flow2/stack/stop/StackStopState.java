package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackStopState implements FlowState<StackStopState, StackStopEvent> {
    INIT_STATE,
    STOP_FAILED_STATE,
    STOP_STATE,
    STOP_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
