package com.sequenceiq.datalake.flow.create;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxCreateState implements FlowState {
    INIT_STATE,
    SDX_CREATION_STORAGE_VALIDATION_STATE,
    SDX_CREATION_WAIT_ENV_STATE,
    SDX_CREATION_WAIT_RDS_STATE,
    SDX_CREATION_START_STATE,
    SDX_STACK_CREATION_IN_PROGRESS_STATE,
    SDX_CREATION_FAILED_STATE,
    SDX_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxCreateState() {
    }

    SdxCreateState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
