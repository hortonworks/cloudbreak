package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackSyncState implements FlowState<StackSyncState, StackSyncEvent> {
    INIT_STATE(),
    SYNC_FAILED_STATE(StackSyncFailedAction.class),
    SYNC_STATE(StackSyncAction.class, StackSyncEvent.SYNC_FAILURE_EVENT),
    SYNC_FINISHED_STATE(StackSyncFinishedAction.class, StackSyncEvent.SYNC_FAILURE_EVENT),
    FINAL_STATE();

    private Class<?> action;
    private StackSyncEvent failureEvent;
    private StackSyncState failureState;

    StackSyncState() {
    }

    StackSyncState(Class<?> action) {
        this.action = action;
    }

    StackSyncState(Class<?> action, StackSyncEvent failureEvent) {
        this.action = action;
        this.failureEvent = failureEvent;
    }

    StackSyncState(Class<?> action, StackSyncEvent failureEvent, StackSyncState failureState) {
        this.action = action;
        this.failureEvent = failureEvent;
        this.failureState = failureState;
    }

    @Override
    public Class<?> action() {
        return action;
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
