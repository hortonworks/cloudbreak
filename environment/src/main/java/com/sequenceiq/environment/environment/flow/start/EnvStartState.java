package com.sequenceiq.environment.environment.flow.start;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvStartState implements FlowState {

    INIT_STATE,
    START_DATAHUB_STATE,
    START_DATALAKE_STATE,
    START_FREEIPA_STATE,
    SYNCHRONIZE_USERS_STATE,
    ENV_START_FINISHED_STATE,
    ENV_START_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
