package com.sequenceiq.datalake.flow.upgrade;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxUpgradeState implements FlowState {

    INIT_STATE,
    SDX_UPGRADE_START_STATE,
    SDX_IMAGE_CHANGED_STATE,
    SDX_UPGRADE_IN_PROGRESS_STATE,
    SDX_UPGRADE_FAILED_STATE,
    SDX_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = DefaultRestartAction.class;

    SdxUpgradeState() {
    }

    SdxUpgradeState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
