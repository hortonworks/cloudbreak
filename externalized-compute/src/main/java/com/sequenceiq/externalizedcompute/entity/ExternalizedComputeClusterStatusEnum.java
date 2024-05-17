package com.sequenceiq.externalizedcompute.entity;

public enum ExternalizedComputeClusterStatusEnum {

    CREATE_IN_PROGRESS,
    REINITIALIZE_IN_PROGRESS,
    DELETE_IN_PROGRESS,
    DELETED,
    AVAILABLE,
    CREATE_FAILED,
    DELETE_FAILED,
    UNKNOWN;

    public boolean isDeleteInProgressOrCompleted() {
        return DELETE_IN_PROGRESS.equals(this) || DELETED.equals(this);
    }

    public boolean isDeleteInProgressOrCompletedOrFailedOrReinitializeInProgress() {
        return DELETE_IN_PROGRESS.equals(this) || DELETED.equals(this) || DELETE_FAILED.equals(this) || REINITIALIZE_IN_PROGRESS.equals(this);
    }

    public boolean isFailed() {
        return CREATE_FAILED.equals(this) || DELETE_FAILED.equals(this);
    }

    public boolean isInProgress() {
        return CREATE_IN_PROGRESS.equals(this) || REINITIALIZE_IN_PROGRESS.equals(this) || DELETE_IN_PROGRESS.equals(this);
    }
}
