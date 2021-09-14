package com.sequenceiq.datalake.flow.datalake.cmsync;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

public enum SdxCmSyncState implements FlowState {
    INIT_STATE,
    CORE_CM_SYNC_STATE,
    CORE_CM_SYNC_IN_PROGRESS_STATE,
    SDX_CDH_VERSION_UPDATE_STATE,
    SDX_CM_SYNC_FINISHED_STATE,
    SDX_CM_SYNC_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends DefaultRestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
