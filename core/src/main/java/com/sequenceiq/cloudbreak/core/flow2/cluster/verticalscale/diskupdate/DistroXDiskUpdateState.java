package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DistroXDiskUpdateState implements FlowState {

    INIT_STATE,
    DATAHUB_DISK_UPDATE_VALIDATION_STATE,
    DATAHUB_DISK_UPDATE_STATE,
    DISK_RESIZE_STATE,
    DATAHUB_DISK_UPDATE_FINISHED_STATE,
    DATAHUB_DISK_UPDATE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
