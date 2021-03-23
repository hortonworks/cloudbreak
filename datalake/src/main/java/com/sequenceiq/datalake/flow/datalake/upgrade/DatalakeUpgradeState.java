package com.sequenceiq.datalake.flow.datalake.upgrade;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum DatalakeUpgradeState implements FlowState {

    INIT_STATE,
    DATALAKE_UPGRADE_START_STATE,
    DATALAKE_UPGRADE_DETERMINE_IMAGE_STATE,
    DATALAKE_IMAGE_CHANGE_STATE,
    DATALAKE_IMAGE_CHANGE_IN_PROGRESS_STATE,
    DATALAKE_REPLACE_VMS_STATE,
    DATALAKE_REPLACE_VMS_IN_PROGRESS_STATE,
    DATALAKE_UPGRADE_IN_PROGRESS_STATE,
    DATALAKE_UPGRADE_COULD_NOT_START_STATE,
    DATALAKE_UPGRADE_FAILED_STATE,
    DATALAKE_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    DatalakeUpgradeState() {
    }

    DatalakeUpgradeState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
