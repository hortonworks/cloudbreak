package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.flow.core.FlowState;

public enum DatabaseBackupState implements FlowState {
    INIT_STATE,
    DATABASE_BACKUP_IN_PROGRESS_STATE,
    FULL_BACKUP_IN_PROGRESS_STATE,
    BACKUP_FAILED_STATE,
    BACKUP_FINISHED_STATE,
    FINAL_STATE;
}
