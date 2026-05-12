package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DiskSyncState implements FlowState {
    INIT_STATE,
    DISK_SYNC_INIT_STATE,
    DISK_SYNC_FINISHED_STATE,
    FINAL_STATE,
    DISK_SYNC_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
