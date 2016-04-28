package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackSyncState implements FlowState<StackSyncState, StackSyncEvent> {
    INIT_STATE,
    SYNC_FAILED_STATE,
    SYNC_STATE,
    SYNC_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<?> action() {
        return null;
    }
}
