package com.sequenceiq.datalake.flow.upgrade.database;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxUpgradeDatabaseServerState implements FlowState {

    INIT_STATE,
    SDX_UPGRADE_DATABASE_SERVER_UPGRADE_STATE,
    SDX_UPGRADE_DATABASE_SERVER_FINISHED_STATE,
    SDX_UPGRADE_DATABASE_SERVER_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    SdxUpgradeDatabaseServerState() {
    }

    SdxUpgradeDatabaseServerState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }

}
