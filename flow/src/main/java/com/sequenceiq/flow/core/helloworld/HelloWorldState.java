package com.sequenceiq.flow.core.helloworld;

import com.sequenceiq.flow.core.FlowState;

public enum HelloWorldState implements FlowState {
    INIT_STATE,
    HELLO_WORLD_START_STATE,
    HELLO_WORLD_FINISHED_STATE,
    HELLO_WORLD_FAILED_STATE,
    FINAL_STATE
}
