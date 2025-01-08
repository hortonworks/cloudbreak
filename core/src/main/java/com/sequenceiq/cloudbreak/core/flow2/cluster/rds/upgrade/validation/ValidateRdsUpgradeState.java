package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum ValidateRdsUpgradeState implements FlowState {

    INIT_STATE,
    VALIDATE_RDS_UPGRADE_FAILED_STATE,
    VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_STATE,
    VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_STATE,
    VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE,
    WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_STATE,
    VALIDATE_RDS_UPGRADE_CONNECTION_STATE,
    VALIDATE_RDS_UPGRADE_CLEANUP_STATE,
    WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_STATE,
    VALIDATE_RDS_UPGRADE_FINISHED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    ValidateRdsUpgradeState() {

    }

    ValidateRdsUpgradeState(Class<? extends DefaultRestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}