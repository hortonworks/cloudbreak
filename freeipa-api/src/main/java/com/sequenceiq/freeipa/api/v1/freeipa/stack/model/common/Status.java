package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import java.util.Collection;
import java.util.List;

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
    UNREACHABLE,
    UNHEALTHY,
    DELETED_ON_PROVIDER_SIDE,
    UNKNOWN;

    public static final Collection<Status> AVAILABLE_STATUSES = List.of(AVAILABLE, MAINTENANCE_MODE_ENABLED);

    public static final Collection<Status> REMOVABLE_STATUSES = List.of(AVAILABLE, UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED,
            DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED);

    public static final Collection<Status> FAILED_STATUSES = List.of(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED,
            STOP_FAILED);

    public boolean isRemovableStatus() {
        return REMOVABLE_STATUSES.contains(this);
    }

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }

    public boolean isAvailable() {
        return AVAILABLE_STATUSES.contains(this);
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
