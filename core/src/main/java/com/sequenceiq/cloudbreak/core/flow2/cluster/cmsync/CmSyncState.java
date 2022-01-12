package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum CmSyncState implements FlowState {
    INIT_STATE,
    CM_SYNC_FAILED_STATE,

    CM_SYNC_STATE,
    CM_SYNC_FINISHED_STATE,

    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
