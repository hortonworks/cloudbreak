package com.sequenceiq.datalake.flow.dr.backup;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeBackupState implements FlowState {

    INIT_STATE,
    DATALAKE_DATABASE_BACKUP_START_STATE,
    DATALAKE_TRIGGERING_BACKUP_STATE,
    DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE,
    DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE,
    DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE,
    DATALAKE_DATABASE_BACKUP_FAILED_STATE,
    DATALAKE_BACKUP_FAILED_STATE,
    DATALAKE_BACKUP_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeBackupState() {
    }

    DatalakeBackupState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
