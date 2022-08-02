package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpgradeEmbeddedDBPreparationState implements FlowState {
    INIT_STATE,
    UPGRADE_EMBEDDED_DB_PREPARATION_FAILED_STATE,
    UPGRADE_EMBEDDED_DB_PREPARATION_STATE,
    UPGRADE_EMBEDDED_DB_PREPARATION_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    UpgradeEmbeddedDBPreparationState() {

    }

    UpgradeEmbeddedDBPreparationState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
