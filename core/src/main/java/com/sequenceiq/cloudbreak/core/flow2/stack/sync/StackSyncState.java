package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackSyncState implements FlowState<StackSyncState, StackSyncEvent> {
    INIT_STATE,
    SYNC_FAILED_STATE,
    SYNC_STATE(StackSyncEvent.SYNC_FAILURE_EVENT),
    SYNC_FINISHED_STATE(StackSyncEvent.SYNC_FAILURE_EVENT),
    FINAL_STATE;

    private StackSyncEvent failureEvent;
    private StackSyncState failureState;

    StackSyncState() {
    }

    StackSyncState(StackSyncEvent failureEvent) {
        this.failureEvent = failureEvent;
    }

    @Override
    public Class<?> action() {
        return null;
    }

    @Override
    public StackSyncState failureState() {
        return failureState;
    }

    @Override
    public StackSyncEvent failureEvent() {
        return failureEvent;
    }
}
