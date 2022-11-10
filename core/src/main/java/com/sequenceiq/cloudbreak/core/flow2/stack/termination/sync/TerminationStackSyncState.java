package com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum TerminationStackSyncState implements FlowState {
    INIT_STATE,
    TERMINATION_SYNC_FAILED_STATE,
    TERMINATION_SYNC_STATE,
    TERMINATION_SYNC_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
