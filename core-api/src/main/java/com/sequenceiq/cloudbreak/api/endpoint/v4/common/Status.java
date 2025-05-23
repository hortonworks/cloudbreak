package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static java.lang.String.format;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.StatusKind;

public enum Status {
    REQUESTED(StatusKind.PROGRESS),
    CREATE_IN_PROGRESS(StatusKind.PROGRESS),
    AVAILABLE(StatusKind.FINAL),
    DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS(StatusKind.PROGRESS),
    DATAHUB_DISK_UPDATE_VALIDATION_FAILED(StatusKind.FINAL),
    DATAHUB_DISK_UPDATE_FAILED(StatusKind.FINAL),
    DATAHUB_DISK_UPDATE_RESIZE_FAILED(StatusKind.FINAL),
    UPDATE_IN_PROGRESS(StatusKind.PROGRESS),
    UPDATE_REQUESTED(StatusKind.PROGRESS),
    UPDATE_FAILED(StatusKind.FINAL),
    BACKUP_IN_PROGRESS(StatusKind.PROGRESS),
    BACKUP_FAILED(StatusKind.FINAL),
    BACKUP_FINISHED(StatusKind.FINAL),
    RESTORE_IN_PROGRESS(StatusKind.PROGRESS),
    RESTORE_FAILED(StatusKind.FINAL),
    RESTORE_FINISHED(StatusKind.FINAL),
    RECOVERY_IN_PROGRESS(StatusKind.PROGRESS),
    RECOVERY_REQUESTED(StatusKind.PROGRESS),
    RECOVERY_FAILED(StatusKind.FINAL),
    CREATE_FAILED(StatusKind.FINAL),
    ENABLE_SECURITY_FAILED(StatusKind.FINAL),
    PRE_DELETE_IN_PROGRESS(StatusKind.PROGRESS),
    DELETE_IN_PROGRESS(StatusKind.PROGRESS),
    DELETE_FAILED(StatusKind.FINAL),
    DELETED_ON_PROVIDER_SIDE(StatusKind.FINAL),
    DELETE_COMPLETED(StatusKind.FINAL),
    STOPPED(StatusKind.FINAL),
    @Deprecated
    STOP_REQUESTED(StatusKind.PROGRESS),
    @Deprecated
    START_REQUESTED(StatusKind.PROGRESS),
    STOP_IN_PROGRESS(StatusKind.PROGRESS),
    START_IN_PROGRESS(StatusKind.PROGRESS),
    START_FAILED(StatusKind.FINAL),
    STOP_FAILED(StatusKind.FINAL),
    WAIT_FOR_SYNC(StatusKind.PROGRESS),
    MAINTENANCE_MODE_ENABLED(StatusKind.FINAL),
    AMBIGUOUS(StatusKind.FINAL),
    UNREACHABLE(StatusKind.FINAL),
    NODE_FAILURE(StatusKind.FINAL),
    EXTERNAL_DATABASE_CREATION_IN_PROGRESS(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_CREATION_FAILED(StatusKind.FINAL),
    EXTERNAL_DATABASE_DELETION_IN_PROGRESS(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_DELETION_FINISHED(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_DELETION_FAILED(StatusKind.FINAL),
    EXTERNAL_DATABASE_START_IN_PROGRESS(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_START_FINISHED(StatusKind.FINAL),
    EXTERNAL_DATABASE_START_FAILED(StatusKind.FINAL),
    EXTERNAL_DATABASE_STOP_IN_PROGRESS(StatusKind.PROGRESS),
    @Deprecated
    EXTERNAL_DATABASE_STOP_FINISHED(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_STOP_FAILED(StatusKind.FINAL),
    EXTERNAL_DATABASE_UPGRADE_IN_PROGRESS(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_UPGRADE_FINISHED(StatusKind.PROGRESS),
    EXTERNAL_DATABASE_UPGRADE_FAILED(StatusKind.FINAL),
    LOAD_BALANCER_UPDATE_IN_PROGRESS(StatusKind.PROGRESS),
    LOAD_BALANCER_UPDATE_FINISHED(StatusKind.FINAL),
    LOAD_BALANCER_UPDATE_FAILED(StatusKind.FINAL),
    UPGRADE_CCM_IN_PROGRESS(StatusKind.PROGRESS),
    UPGRADE_CCM_FAILED(StatusKind.FINAL),
    UPGRADE_CCM_FINISHED(StatusKind.FINAL),
    DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS(StatusKind.PROGRESS),
    STALE(StatusKind.FINAL);

    private static final Map<Status, Status> IN_PROGRESS_TO_FINAL_STATUS_MAPPING = ImmutableMap.<Status, Status>builder()
            .put(REQUESTED, CREATE_FAILED)
            .put(CREATE_IN_PROGRESS, CREATE_FAILED)
            .put(DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS, UPDATE_FAILED)
            .put(UPDATE_IN_PROGRESS, UPDATE_FAILED)
            .put(UPDATE_REQUESTED, UPDATE_FAILED)
            .put(BACKUP_IN_PROGRESS, BACKUP_FAILED)
            .put(RESTORE_IN_PROGRESS, RESTORE_FAILED)
            .put(RECOVERY_IN_PROGRESS, RECOVERY_FAILED)
            .put(RECOVERY_REQUESTED, RECOVERY_FAILED)
            .put(PRE_DELETE_IN_PROGRESS, DELETE_FAILED)
            .put(DELETE_IN_PROGRESS, DELETE_FAILED)
            .put(STOP_REQUESTED, STOP_FAILED)
            .put(STOP_IN_PROGRESS, STOP_FAILED)
            .put(START_REQUESTED, START_FAILED)
            .put(START_IN_PROGRESS, START_FAILED)
            .put(WAIT_FOR_SYNC, AVAILABLE)
            .put(EXTERNAL_DATABASE_CREATION_IN_PROGRESS, CREATE_FAILED)
            .put(EXTERNAL_DATABASE_DELETION_IN_PROGRESS, DELETE_FAILED)
            .put(EXTERNAL_DATABASE_DELETION_FINISHED, DELETE_FAILED)
            .put(EXTERNAL_DATABASE_START_IN_PROGRESS, START_FAILED)
            .put(EXTERNAL_DATABASE_START_FINISHED, START_FAILED)
            .put(EXTERNAL_DATABASE_STOP_IN_PROGRESS, STOP_FAILED)
            .put(EXTERNAL_DATABASE_STOP_FINISHED, STOP_FAILED)
            .put(EXTERNAL_DATABASE_UPGRADE_IN_PROGRESS, EXTERNAL_DATABASE_UPGRADE_FAILED)
            .put(EXTERNAL_DATABASE_UPGRADE_FINISHED, EXTERNAL_DATABASE_UPGRADE_FAILED)
            .put(LOAD_BALANCER_UPDATE_IN_PROGRESS, UPDATE_FAILED)
            .put(UPGRADE_CCM_IN_PROGRESS, UPGRADE_CCM_FAILED)
            .put(DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS, AVAILABLE)
            .build();

    private final StatusKind statusKind;

    Status(StatusKind statusKind) {
        this.statusKind = statusKind;
    }

    public static Set<Status> getAvailableStatuses() {
        return Sets.immutableEnumSet(AVAILABLE, MAINTENANCE_MODE_ENABLED);
    }

    public static Set<Status> getStoppedStatuses() {
        return Sets.immutableEnumSet(STOPPED);
    }

    public static Set<Status> getAllowedDataHubStatesForSdxUpgrade() {
        return Sets.immutableEnumSet(STOPPED, DELETE_COMPLETED,
                CREATE_FAILED, DELETE_FAILED, DELETED_ON_PROVIDER_SIDE);
    }

    public static EnumSet<Status> getUnschedulableStatuses() {
        return EnumSet.of(
                CREATE_FAILED,
                PRE_DELETE_IN_PROGRESS,
                DELETE_IN_PROGRESS,
                DELETE_FAILED,
                DELETE_COMPLETED,
                EXTERNAL_DATABASE_CREATION_FAILED,
                EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                EXTERNAL_DATABASE_DELETION_FINISHED,
                EXTERNAL_DATABASE_DELETION_FAILED,
                STALE
        );
    }

    public static boolean isClusterAvailable(Status stackStatus, Status clusterStatus) {
        return getAvailableStatuses().contains(stackStatus) && getAvailableStatuses().contains(clusterStatus);
    }

    public StatusKind getStatusKind() {
        return statusKind;
    }

    public boolean isRemovableStatus() {
        return EnumSet.of(AVAILABLE, UPDATE_FAILED, RECOVERY_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED,
                DELETE_COMPLETED, DELETED_ON_PROVIDER_SIDE, STOPPED, START_FAILED, STOP_FAILED, UPGRADE_CCM_FAILED, STALE).contains(this);
    }

    public boolean isAvailable() {
        return getAvailableStatuses().contains(this);
    }

    public boolean isInProgress() {
        return getStatusKind().equals(StatusKind.PROGRESS);
    }

    public boolean isStopped() {
        return STOPPED == this;
    }

    public boolean isStopState() {
        return STOPPED.equals(this)
                || STOP_IN_PROGRESS.equals(this)
                || STOP_REQUESTED.equals(this);
    }

    public boolean isTerminatedOrDeletionInProgress() {
        return DELETE_COMPLETED == this
                || DELETE_FAILED == this
                || DELETE_IN_PROGRESS == this
                || DELETED_ON_PROVIDER_SIDE == this
                || PRE_DELETE_IN_PROGRESS == this;
    }

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
}
