package com.sequenceiq.freeipa.api.v1.operation.model;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

public enum OperationState {
    REQUESTED, RUNNING, COMPLETED, FAILED, REJECTED, TIMEDOUT;

    public static OperationState fromSynchronizationStatus(SynchronizationStatus synchronizationStatus) {
        switch (synchronizationStatus) {
            case REQUESTED:
                return REQUESTED;
            case RUNNING:
                return RUNNING;
            case COMPLETED:
                return COMPLETED;
            case FAILED:
                return FAILED;
            case REJECTED:
                return REJECTED;
            case TIMEDOUT:
                return TIMEDOUT;
            default:
                throw new UnsupportedOperationException("SynchronizationStatus not mapped: " + synchronizationStatus);
        }
    }
}
