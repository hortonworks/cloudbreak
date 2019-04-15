package com.sequenceiq.cloudbreak.core.flow2.helloworld;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum HelloWorldState implements FlowState {
    INIT_STATE,
    HELLO_WORLD_START_STATE,
    HELLO_WORLD_FINISHED_STATE,
    HELLO_WORLD_FAILED_STATE,
    FINAL_STATE
}
