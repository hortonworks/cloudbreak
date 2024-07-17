package com.sequenceiq.datalake.flow.dr.validation;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeRestoreValidationState implements FlowState {

    INIT_STATE,
    DATALAKE_TRIGGERING_RESTORE_VALIDATION_STATE,
    DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_STATE,
    DATALAKE_RESTORE_VALIDATION_FAILED_STATE,
    DATALAKE_RESTORE_VALIDATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeRestoreValidationState() {
    }

    DatalakeRestoreValidationState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
