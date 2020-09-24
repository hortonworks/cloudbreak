package com.sequenceiq.environment.environment.flow.config.update;

import com.sequenceiq.flow.core.FlowState;

public enum EnvStackConfigUpdatesState implements FlowState {
    INIT_STATE,
    STACK_CONFIG_UPDATES_START_STATE,
    STACK_CONFIG_UPDATES_FINISHED_STATE,
    STACK_CONFIG_UPDATES_FAILED_STATE,
    FINAL_STATE;
}
