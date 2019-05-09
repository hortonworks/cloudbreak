package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.flow.core.FlowState;

public enum StackStartState implements FlowState {
    INIT_STATE,
    START_FAILED_STATE,
    START_STATE,
    COLLECTING_METADATA,
    START_FINISHED_STATE,
    FINAL_STATE
}
