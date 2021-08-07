package com.sequenceiq.datalake.flow.datalake.recovery;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeUpgradeRecoveryState implements FlowState {

    INIT_STATE,
    DATALAKE_RECOVERY_START_STATE,
    DATALAKE_RECOVERY_DETERMINE_IMAGE_STATE,
    DATALAKE_RECOVERY_IN_PROGRESS_STATE,
    DATALAKE_RECOVERY_COULD_NOT_START_STATE,
    DATALAKE_RECOVERY_FAILED_STATE,
    DATALAKE_RECOVERY_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeUpgradeRecoveryState() {
    }

    DatalakeUpgradeRecoveryState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
