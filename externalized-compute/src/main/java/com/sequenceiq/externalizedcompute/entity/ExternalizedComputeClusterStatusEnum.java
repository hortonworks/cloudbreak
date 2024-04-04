package com.sequenceiq.externalizedcompute.entity;

public enum ExternalizedComputeClusterStatusEnum {

    CREATE_IN_PROGRESS,
    DELETE_IN_PROGRESS,
    DELETED,
    AVAILABLE,
    CREATE_FAILED,
    DELETE_FAILED,
    UNKNOWN;

    public boolean isDeleteInProgressOrCompleted() {
        return DELETE_IN_PROGRESS.equals(this) || DELETED.equals(this);
    }

    public boolean isDeleteInProgressOrCompletedOrFailed() {
        return DELETE_IN_PROGRESS.equals(this) || DELETED.equals(this) || DELETE_FAILED.equals(this);
    }
}
