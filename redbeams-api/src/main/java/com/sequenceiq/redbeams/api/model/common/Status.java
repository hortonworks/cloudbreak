package com.sequenceiq.redbeams.api.model.common;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

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
    SSL_ROTATED,
    SSL_ROTATE_REQUESTED,
    START_REQUESTED,
    STOP_IN_PROGRESS,
    SSL_ROTATE_IN_PROGRESS,
    START_IN_PROGRESS,
    START_FAILED,
    SSL_ROTATE_FAILED,
    STOP_FAILED,
    WAIT_FOR_SYNC,
    MAINTENANCE_MODE_ENABLED,
    UPGRADE_REQUESTED,
    UPGRADE_IN_PROGRESS,
    UPGRADE_FAILED,
    VALIDATE_UPGRADE_REQUESTED,
    VALIDATE_UPGRADE_IN_PROGRESS,
    VALIDATE_UPGRADE_FAILED,
    DB_SSL_MIGRATION_COMPLETED,
    DB_SSL_MIGRATION_IN_PROGRESS,
    DB_SSL_MIGRATION_FAILED,
    UNKNOWN;

    private static final Map<Status, Status> IN_PROGRESS_TO_FINAL_STATUS_MAPPING = ImmutableMap.<Status, Status>builder()
            .put(REQUESTED, CREATE_FAILED)
            .put(UPDATE_REQUESTED, UPDATE_FAILED)
            .put(DELETE_REQUESTED, DELETE_FAILED)
            .put(STOP_REQUESTED, STOP_FAILED)
            .put(START_REQUESTED, START_FAILED)
            .put(CREATE_IN_PROGRESS, CREATE_FAILED)
            .put(UPDATE_IN_PROGRESS, UPDATE_FAILED)
            .put(PRE_DELETE_IN_PROGRESS, DELETE_FAILED)
            .put(DELETE_IN_PROGRESS, DELETE_FAILED)
            .put(STOP_IN_PROGRESS, STOP_FAILED)
            .put(START_IN_PROGRESS, START_FAILED)
            .put(UPGRADE_REQUESTED, UPGRADE_FAILED)
            .put(UPGRADE_IN_PROGRESS, UPGRADE_FAILED)
            .put(VALIDATE_UPGRADE_REQUESTED, VALIDATE_UPGRADE_FAILED)
            .put(VALIDATE_UPGRADE_IN_PROGRESS, VALIDATE_UPGRADE_FAILED)
            .put(WAIT_FOR_SYNC, AVAILABLE)
            .put(SSL_ROTATE_REQUESTED, SSL_ROTATE_FAILED)
            .put(SSL_ROTATE_IN_PROGRESS, SSL_ROTATE_FAILED)
            .put(DB_SSL_MIGRATION_IN_PROGRESS, DB_SSL_MIGRATION_FAILED)
            .build();

    private static final List<Status> IS_AVAILABLE_LIST = Arrays.asList(
            AVAILABLE, MAINTENANCE_MODE_ENABLED, SSL_ROTATED, VALIDATE_UPGRADE_FAILED, DB_SSL_MIGRATION_COMPLETED);

    public boolean isAvailable() {
        return IS_AVAILABLE_LIST.contains(valueOf(name()));
    }

    public boolean isStopPhaseActive() {
        return name().contains("STOP");
    }

    public boolean isStopped() {
        return name().equals("STOPPED");
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

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(this);
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

    public boolean isUpgradeInProgress() {
        return UPGRADE_REQUESTED.equals(this)
                || UPGRADE_IN_PROGRESS.equals(this)
                || VALIDATE_UPGRADE_IN_PROGRESS.equals(this)
                || VALIDATE_UPGRADE_REQUESTED.equals(this);
    }

    public static Set<Status> getAutoSyncStatuses() {
        return Set.of(
                START_REQUESTED,
                START_IN_PROGRESS,
                AVAILABLE,
                STOP_REQUESTED,
                STOP_IN_PROGRESS,
                STOPPED,
                DELETE_IN_PROGRESS
        );
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    public boolean isInProgress() {
        return switch (this) {
            case REQUESTED,
                 UPDATE_REQUESTED,
                 DELETE_REQUESTED,
                 STOP_REQUESTED,
                 START_REQUESTED,
                 CREATE_IN_PROGRESS,
                 UPDATE_IN_PROGRESS,
                 PRE_DELETE_IN_PROGRESS,
                 DELETE_IN_PROGRESS,
                 STOP_IN_PROGRESS,
                 START_IN_PROGRESS,
                 UPGRADE_REQUESTED,
                 UPGRADE_IN_PROGRESS,
                 VALIDATE_UPGRADE_REQUESTED,
                 VALIDATE_UPGRADE_IN_PROGRESS,
                 WAIT_FOR_SYNC,
                 SSL_ROTATE_IN_PROGRESS,
                 SSL_ROTATE_REQUESTED,
                 DB_SSL_MIGRATION_IN_PROGRESS -> true;
            default -> false;
        };
    }
    //CHECKSTYLE:ON: CyclomaticComplexity

    public Status mapToFailedIfInProgress() {
        if (isInProgress()) {
            Status result = IN_PROGRESS_TO_FINAL_STATUS_MAPPING.get(this);
            if (result == null) {
                throw new IllegalArgumentException(format("Status '%s' is not mappable to failed state.", this));
            } else {
                return result;
            }
        } else {
            return this;
        }
    }

    public static Set<Status> getUnscheduleAutoSyncStatuses() {
        return Set.of(DELETE_COMPLETED);
    }
}