package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import java.util.Arrays;

public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    STACK_AVAILABLE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    CREATE_FAILED,
    ENABLE_SECURITY_FAILED,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETED,
    STOPPED,
    STOP_REQUESTED,
    START_REQUESTED,
    STOP_IN_PROGRESS,
    START_IN_PROGRESS,
    START_FAILED,
    STOP_FAILED,
    WAIT_FOR_SYNC,
    MAINTENANCE_MODE_ENABLED,
    UNKNOWN;

    public boolean isRemovableStatus() {
        return Arrays.asList(AVAILABLE, UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED,
                DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED).contains(this);
    }

    public boolean isFailed() {
        return Arrays.asList(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED)
                .contains(this);
    }

    public boolean isAvailable() {
        return Arrays.asList(AVAILABLE, MAINTENANCE_MODE_ENABLED).contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return DELETE_COMPLETED.equals(this);
    }

    public boolean isDeletionInProgress() {
        return DELETE_IN_PROGRESS.equals(this);
    }

    public boolean isStopPhaseActive() {
        return name().contains("STOP");
    }
}
