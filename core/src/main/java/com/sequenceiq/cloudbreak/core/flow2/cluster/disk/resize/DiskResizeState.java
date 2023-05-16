package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DiskResizeState implements FlowState {
    INIT_STATE,
    DISK_UPDATE_STATE,
    DISK_UPDATE_FINISHED_STATE,
    FINAL_STATE,
    DISK_UPDATE_FAILED_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
