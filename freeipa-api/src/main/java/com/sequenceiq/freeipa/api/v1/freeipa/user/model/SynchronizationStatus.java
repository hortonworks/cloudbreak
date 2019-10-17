package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;

public enum SynchronizationStatus {
    REQUESTED, RUNNING, COMPLETED, FAILED, REJECTED, TIMEDOUT;

    public static SynchronizationStatus fromOperationState(OperationState operationState) {
        switch (operationState) {
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
                throw new UnsupportedOperationException("OperationState not mapped: " + operationState);
        }
    }
}
