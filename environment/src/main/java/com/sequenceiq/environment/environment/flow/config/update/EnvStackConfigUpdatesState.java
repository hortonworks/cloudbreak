package com.sequenceiq.environment.environment.flow.config.update;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvStackConfigUpdatesState implements FlowState {
    INIT_STATE,
    STACK_CONFIG_UPDATES_START_STATE,
    STACK_CONFIG_UPDATES_FINISHED_STATE,
    STACK_CONFIG_UPDATES_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
