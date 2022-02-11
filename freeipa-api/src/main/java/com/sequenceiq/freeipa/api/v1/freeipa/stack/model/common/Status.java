package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import java.util.Collection;
import java.util.List;

//if statuses are added in this enum class, please also add them in cloudbreak-ui repository
//https://github.com/hortonworks/hortonworks-cloud/blob/master/web/cloudbreak-ui/src/app/helpers/freeipa.helpers.ts
public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    AVAILABLE,
    STACK_AVAILABLE,
    DIAGNOSTICS_COLLECTION_IN_PROGRESS,
    UPDATE_IN_PROGRESS,
    UPDATE_REQUESTED,
    UPDATE_FAILED,
    UPSCALE_FAILED,
    DOWNSCALE_FAILED,
    REPAIR_FAILED,
    CREATE_FAILED,
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
    UNKNOWN,
    // CCM upgrade statuses
    UPGRADE_CCM_REQUESTED,
    UPGRADE_CCM_IN_PROGRESS,
    UPGRADE_CCM_FAILED;

    public static final Collection<Status> REMOVABLE_STATUSES = List.of(AVAILABLE, UPDATE_FAILED, CREATE_FAILED, DELETE_FAILED,
            DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED, REPAIR_FAILED, UPSCALE_FAILED, DOWNSCALE_FAILED, UPGRADE_CCM_FAILED);

    public static final Collection<Status> FAILED_STATUSES = List.of(UPDATE_FAILED, CREATE_FAILED, DELETE_FAILED, START_FAILED,
            STOP_FAILED, REPAIR_FAILED, UPSCALE_FAILED, DOWNSCALE_FAILED, UPGRADE_CCM_FAILED);

    public static final Collection<Status> FREEIPA_UNREACHABLE_STATUSES = List.of(REQUESTED, UNREACHABLE, STOPPED, DELETED_ON_PROVIDER_SIDE,
            DELETE_IN_PROGRESS, DELETE_COMPLETED);

    public static final Collection<Status> FREEIPA_STOPPABLE_STATUSES = List.of(AVAILABLE, STOP_FAILED, START_FAILED);

    public static final Collection<Status> FREEIPA_STARTABLE_STATUSES = List.of(STOPPED, STOP_FAILED, START_FAILED);

    public static final Collection<Status> FREEIPA_START_IN_PROGRESS_STATUSES = List.of(START_IN_PROGRESS);

    public static final Collection<Status> FREEIPA_STOP_IN_PROGRESS_STATUSES = List.of(STOP_IN_PROGRESS);

    public static final Collection<Status> FREEIPA_STOPPED_STATUSES = List.of(STOPPED);

    public boolean isRemovableStatus() {
        return REMOVABLE_STATUSES.contains(this);
    }

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return DELETE_COMPLETED.equals(this);
    }

    public boolean isDeletionInProgress() {
        return DELETE_IN_PROGRESS.equals(this);
    }

    public boolean isFreeIpaUnreachableStatus() {
        return FREEIPA_UNREACHABLE_STATUSES.contains(this);
    }

    public Boolean isStoppable() {
        return FREEIPA_STOPPABLE_STATUSES.contains(this);
    }

    public Boolean isStartable() {
        return FREEIPA_STARTABLE_STATUSES.contains(this);
    }

    public Boolean isStartInProgressPhase() {
        return FREEIPA_START_IN_PROGRESS_STATUSES.contains(this);
    }

    public Boolean isStopInProgressPhase() {
        return FREEIPA_STOP_IN_PROGRESS_STATUSES.contains(this);
    }

    public Boolean isStoppedPhase() {
        return FREEIPA_STOPPED_STATUSES.contains(this);
    }
}
