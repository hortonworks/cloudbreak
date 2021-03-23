package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ExternalDatabaseStartState implements FlowState {
    INIT_STATE,
    EXTERNAL_DATABASE_STARTING_STATE,
    EXTERNAL_DATABASE_START_FAILED_STATE,
    EXTERNAL_DATABASE_START_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
