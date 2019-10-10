package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.model.StatusKind;

public enum Status {
    REQUESTED(StatusKind.PROGRESS),
    CREATE_IN_PROGRESS(StatusKind.PROGRESS),
    AVAILABLE(StatusKind.FINAL),
    UPDATE_IN_PROGRESS(StatusKind.PROGRESS),
    UPDATE_REQUESTED(StatusKind.PROGRESS),
    UPDATE_FAILED(StatusKind.FINAL),
    CREATE_FAILED(StatusKind.FINAL),
    ENABLE_SECURITY_FAILED(StatusKind.FINAL),
    PRE_DELETE_IN_PROGRESS(StatusKind.PROGRESS),
    DELETE_IN_PROGRESS(StatusKind.PROGRESS),
    DELETE_FAILED(StatusKind.FINAL),
    DELETE_COMPLETED(StatusKind.FINAL),
    STOPPED(StatusKind.FINAL),
    STOP_REQUESTED(StatusKind.PROGRESS),
    START_REQUESTED(StatusKind.PROGRESS),
    STOP_IN_PROGRESS(StatusKind.PROGRESS),
    START_IN_PROGRESS(StatusKind.PROGRESS),
    START_FAILED(StatusKind.FINAL),
    STOP_FAILED(StatusKind.FINAL),
    WAIT_FOR_SYNC(StatusKind.PROGRESS),
    MAINTENANCE_MODE_ENABLED(StatusKind.FINAL);

    private StatusKind statusKind;

    Status(StatusKind statusKind) {
        this.statusKind = statusKind;
    }

    public StatusKind getStatusKind() {
        return statusKind;
    }

    public boolean isRemovableStatus() {
        return Arrays.asList(AVAILABLE, UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED,
                DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED).contains(valueOf(name()));
    }

    public boolean isAvailable() {
        return Arrays.asList(AVAILABLE, MAINTENANCE_MODE_ENABLED).contains(valueOf(name()));
    }

    public boolean isInProgress() {
        return getStatusKind().equals(StatusKind.PROGRESS);
    }

    public boolean isStopped() {
        return STOPPED == this;
    }

    public boolean isStartState() {
        return Status.AVAILABLE.equals(this)
                || UPDATE_IN_PROGRESS.equals(this)
                || Status.START_FAILED.equals(this)
                || Status.START_REQUESTED.equals(this)
                || Status.START_IN_PROGRESS.equals(this);
    }

    public boolean isStopState() {
        return Status.STOPPED.equals(this)
                || UPDATE_IN_PROGRESS.equals(this)
                || Status.STOP_IN_PROGRESS.equals(this)
                || Status.STOP_REQUESTED.equals(this)
                || Status.STOP_FAILED.equals(this);
    }
}
