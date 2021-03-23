package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatabaseBackupState implements FlowState {
    INIT_STATE,
    DATABASE_BACKUP_STATE,
    DATABASE_BACKUP_FAILED_STATE,
    DATABASE_BACKUP_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
