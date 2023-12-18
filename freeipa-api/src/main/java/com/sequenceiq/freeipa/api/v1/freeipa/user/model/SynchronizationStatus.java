package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;

public enum SynchronizationStatus {
    REQUESTED, RUNNING, COMPLETED, FAILED, REJECTED, TIMEDOUT;

    public static SynchronizationStatus fromOperationState(OperationState operationState) {
        return switch (operationState) {
            case REQUESTED -> REQUESTED;
            case RUNNING -> RUNNING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
            case REJECTED -> REJECTED;
            case TIMEDOUT -> TIMEDOUT;
            default -> throw new UnsupportedOperationException("OperationState not mapped: " + operationState);
        };
    }
}
