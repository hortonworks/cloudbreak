package com.sequenceiq.datalake.flow.dr.validation;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeBackupValidationState implements FlowState {

    INIT_STATE,
    DATALAKE_TRIGGERING_BACKUP_VALIDATION_STATE,
    DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_STATE,
    DATALAKE_BACKUP_VALIDATION_FAILED_STATE,
    DATALAKE_BACKUP_VALIDATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeBackupValidationState() {
    }

    DatalakeBackupValidationState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
