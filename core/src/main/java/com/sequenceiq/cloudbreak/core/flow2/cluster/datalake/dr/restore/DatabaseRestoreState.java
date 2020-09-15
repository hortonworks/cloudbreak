package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import com.sequenceiq.flow.core.FlowState;

public enum DatabaseRestoreState implements FlowState {
    INIT_STATE,
    DATABASE_RESTORE_IN_PROGRESS_STATE,
    FULL_RESTORE_IN_PROGRESS_STATE,
    RESTORE_FAILED_STATE,
    RESTORE_FINISHED_STATE,
    FINAL_STATE;
}
