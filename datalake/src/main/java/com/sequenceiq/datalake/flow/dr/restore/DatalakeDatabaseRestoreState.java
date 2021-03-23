package com.sequenceiq.datalake.flow.dr.restore;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeDatabaseRestoreState implements FlowState {

    INIT_STATE,
    DATALAKE_DATABASE_RESTORE_START_STATE,
    DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE,
    DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE,
    DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE,
    DATALAKE_DATABASE_RESTORE_FAILED_STATE,
    DATALAKE_RESTORE_FAILED_STATE,
    DATALAKE_RESTORE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeDatabaseRestoreState() {
    }

    DatalakeDatabaseRestoreState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
