package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatabaseRestoreState implements FlowState {
    INIT_STATE,
    DATABASE_RESTORE_STATE,
    DATABASE_RESTORE_FAILED_STATE,
    DATABASE_RESTORE_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
