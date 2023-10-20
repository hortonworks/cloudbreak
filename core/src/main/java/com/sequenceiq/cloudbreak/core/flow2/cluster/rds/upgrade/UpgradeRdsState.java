package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum UpgradeRdsState implements FlowState {
    INIT_STATE,
    UPGRADE_RDS_FAILED_STATE,
    UPGRADE_RDS_STOP_SERVICES_STATE,
    UPGRADE_RDS_DATA_BACKUP_STATE,
    UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE,
    UPGRADE_RDS_MIGRATE_DB_SETTINGS_STATE,
    UPGRADE_RDS_DATA_RESTORE_STATE,
    UPGRADE_RDS_START_SERVICES_STATE,

    UPGRADE_RDS_INSTALL_POSTGRES_PACKAGES_STATE,
    UPGRADE_RDS_VERSION_UPDATE_STATE,
    UPGRADE_RDS_FINISHED_STATE,

    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    UpgradeRdsState() {

    }

    UpgradeRdsState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
