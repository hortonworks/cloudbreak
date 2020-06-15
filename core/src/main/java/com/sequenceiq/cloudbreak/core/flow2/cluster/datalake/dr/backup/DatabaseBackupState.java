package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.flow.core.FlowState;

public enum DatabaseBackupState implements FlowState {
    INIT_STATE,
    DATABASE_BACKUP_STATE,
    DATABASE_BACKUP_FAILED_STATE,
    DATABASE_BACKUP_FINISHED_STATE,
    FINAL_STATE;
}
