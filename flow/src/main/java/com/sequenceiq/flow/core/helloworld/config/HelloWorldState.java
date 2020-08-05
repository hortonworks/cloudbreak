package com.sequenceiq.flow.core.helloworld.config;

import com.sequenceiq.flow.core.FlowState;

public enum HelloWorldState implements FlowState {
    INIT_STATE,
    HELLO_WORLD_FIRST_STEP_STATE,
    HELLO_WORLD_FIRST_STEP_FAILED_STATE,
    HELLO_WORLD_SECOND_STEP_STATE,
    HELLO_WORLD_FINISHED_STATE,
    HELLO_WORLD_FAILED_STATE,
    FINAL_STATE
}
