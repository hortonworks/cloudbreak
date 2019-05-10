package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.WaitForSyncRestartAction;

public enum StackSyncState implements FlowState {
    INIT_STATE,
    SYNC_FAILED_STATE,
    SYNC_STATE,
    SYNC_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return WaitForSyncRestartAction.class;
    }
}
