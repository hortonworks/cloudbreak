package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackSyncState implements FlowState<StackSyncState, StackSyncEvent> {
    INIT_STATE,
    SYNC_FAILED_STATE,
    SYNC_STATE,
    SYNC_FINISHED_STATE,
    FINAL_STATE;

    private StackSyncEvent failureEvent;

    @Override
    public Class<?> action() {
        return null;
    }

    @Override
    public StackSyncState failureState() {
        return null;
    }

    @Override
    public StackSyncEvent failureEvent() {
        return failureEvent;
    }

    @Override
    public void setFailureEvent(StackSyncEvent failureEvent) {
        this.failureEvent = failureEvent;
    }
}
