package com.sequenceiq.datalake.flow.verticalscale.diskupdate;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DatalakeDiskUpdateState implements FlowState {

    INIT_STATE,
    DATALAKE_DISK_UPDATE_VALIDATION_STATE,
    DATALAKE_DISK_UPDATE_STATE,
    DATALAKE_DISK_UPDATE_FINISHED_STATE,
    DATALAKE_DISK_UPDATE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
