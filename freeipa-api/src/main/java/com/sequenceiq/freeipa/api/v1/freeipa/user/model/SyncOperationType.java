package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

public enum SyncOperationType {
    USER_SYNC,
    SET_PASSWORD;

    public static SyncOperationType fromOperationType(OperationType operationType) {
        switch (operationType) {
            case USER_SYNC:
                return USER_SYNC;
            case SET_PASSWORD:
                return SET_PASSWORD;
            default:
                throw new UnsupportedOperationException("OperationType not mapped: " + operationType);
        }
    }
}
