package com.sequenceiq.datalake.flow.datalake.upgrade.preparation;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeUpgradePreparationState implements FlowState {
    INIT_STATE,
    DATALAKE_UPGRADE_PREPARATION_START_STATE,
    DATALAKE_UPGRADE_PREPARATION_IN_PROGRESS_STATE,
    DATALAKE_UPGRADE_PREPARATION_FAILED_STATE,
    DATALAKE_UPGRADE_PREPARATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
