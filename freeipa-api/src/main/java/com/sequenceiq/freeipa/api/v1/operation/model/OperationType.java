package com.sequenceiq.freeipa.api.v1.operation.model;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;

public enum OperationType {
    USER_SYNC,
    SET_PASSWORD,
    CLEANUP,
    REPAIR,
    DOWNSCALE;

    public static OperationType fromSyncOperationType(SyncOperationType syncOperationType) {
        switch (syncOperationType) {
            case USER_SYNC:
                return USER_SYNC;
            case SET_PASSWORD:
                return SET_PASSWORD;
            default:
                throw new UnsupportedOperationException("SyncOperationType not mapped: " + syncOperationType);
        }
    }
}
