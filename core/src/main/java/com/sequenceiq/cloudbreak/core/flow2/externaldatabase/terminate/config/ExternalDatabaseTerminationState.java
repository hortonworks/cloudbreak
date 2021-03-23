package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config;

import com.sequenceiq.cloudbreak.core.flow2.restart.InitializeMDCContextRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ExternalDatabaseTerminationState implements FlowState {
    INIT_STATE,
    WAIT_FOR_EXTERNAL_DATABASE_TERMINATION_STATE,
    EXTERNAL_DATABASE_TERMINATION_FAILED_STATE,
    EXTERNAL_DATABASE_TERMINATION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return InitializeMDCContextRestartAction.class;
    }
}
