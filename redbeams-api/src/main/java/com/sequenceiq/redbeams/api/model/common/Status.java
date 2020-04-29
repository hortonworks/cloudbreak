package com.sequenceiq.redbeams.api.model.common;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    CREATE_FAILED,
    ENABLE_SECURITY_FAILED,
    DELETE_REQUESTED,
    PRE_DELETE_IN_PROGRESS,
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

    private static final List<Status> IS_REMOVABLE_STATUS_LIST = Arrays.asList(AVAILABLE, UPDATE_FAILED, CREATE_FAILED,
            ENABLE_SECURITY_FAILED, DELETE_FAILED, DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED);

    private static final List<Status> IS_AVAILABLE_LIST = Arrays.asList(AVAILABLE, MAINTENANCE_MODE_ENABLED);

    public boolean isRemovableStatus() {
        return IS_REMOVABLE_STATUS_LIST.contains(valueOf(name()));
    }

    public boolean isAvailable() {
        return IS_AVAILABLE_LIST.contains(valueOf(name()));
    }

    public boolean isStopPhaseActive() {
        return name().contains("STOP");
    }

    public boolean isSuccessfullyDeleted() {
        return DELETE_COMPLETED.equals(this);
    }

    public boolean isDeleteInProgressOrCompleted() {
        return PRE_DELETE_IN_PROGRESS.equals(this)
                || DELETE_IN_PROGRESS.equals(this)
                || DELETE_COMPLETED.equals(this)
                || DELETE_REQUESTED.equals(this);
    }

    public boolean isDeleteInProgressOrFailed() {
        return PRE_DELETE_IN_PROGRESS.equals(this)
                || DELETE_IN_PROGRESS.equals(this)
                || DELETE_FAILED.equals(this)
                || DELETE_REQUESTED.equals(this);
    }

    public static Set<Status> getDeletingStatuses() {
        return Set.of(PRE_DELETE_IN_PROGRESS, DELETE_REQUESTED, DELETE_FAILED, DELETE_IN_PROGRESS, DELETE_COMPLETED);
    }

    public boolean isStopInProgressOrCompleted() {
        return STOP_REQUESTED.equals(this)
                || STOP_IN_PROGRESS.equals(this)
                || STOPPED.equals(this);
    }

    public boolean isStartInProgressOrCompleted() {
        return START_REQUESTED.equals(this)
                || START_IN_PROGRESS.equals(this)
                || isAvailable();
    }

    public static Set<Status> getAutoSyncStatuses() {
        return Set.of(
                START_REQUESTED,
                START_IN_PROGRESS,
                AVAILABLE,
                STOP_REQUESTED,
                STOP_IN_PROGRESS,
                STOPPED
        );
    }
}
