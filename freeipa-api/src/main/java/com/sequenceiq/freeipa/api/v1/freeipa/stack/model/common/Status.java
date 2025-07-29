package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

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
    VERTICAL_SCALE_FAILED,
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
    UPGRADE_CCM_REQUESTED,
    UPGRADE_CCM_IN_PROGRESS,
    UPGRADE_CCM_FAILED,
    UPGRADE_DEFAULT_OUTBOUND_REQUESTED,
    UPGRADE_DEFAULT_OUTBOUND_IN_PROGRESS,
    UPGRADE_DEFAULT_OUTBOUND_FAILED,
    MODIFY_PROXY_CONFIG_REQUESTED,
    MODIFY_PROXY_CONFIG_IN_PROGRESS,
    MODIFY_PROXY_CONFIG_FAILED,
    UPGRADE_FAILED,
    REBUILD_IN_PROGRESS,
    REBUILD_FAILED,
    STALE,
    TRUST_SETUP_IN_PROGRESS,
    TRUST_SETUP_FINISH_REQUIRED,
    TRUST_SETUP_FAILED,
    TRUST_SETUP_FINISH_SUCCESSFUL,
    TRUST_SETUP_FINISH_FAILED,
    TRUST_SETUP_FINISH_IN_PROGRESS,
    CANCEL_TRUST_SETUP_SUCCESSFUL,
    CANCEL_TRUST_SETUP_FAILED,
    CANCEL_TRUST_SETUP_IN_PROGRESS;

    public static final Collection<Status> REMOVABLE_STATUSES = List.of(AVAILABLE, UPDATE_FAILED, CREATE_FAILED, DELETE_FAILED,
            DELETE_COMPLETED, STOPPED, START_FAILED, STOP_FAILED, REPAIR_FAILED, UPSCALE_FAILED, DOWNSCALE_FAILED, UPGRADE_CCM_FAILED,
            UPGRADE_DEFAULT_OUTBOUND_FAILED, MODIFY_PROXY_CONFIG_FAILED);

    public static final Collection<Status> FAILED_STATUSES = List.of(UPDATE_FAILED, CREATE_FAILED, DELETE_FAILED, START_FAILED,
            STOP_FAILED, REPAIR_FAILED, UPSCALE_FAILED, DOWNSCALE_FAILED, UPGRADE_CCM_FAILED, UPGRADE_DEFAULT_OUTBOUND_FAILED, MODIFY_PROXY_CONFIG_FAILED);

    public static final Collection<Status> FREEIPA_UNREACHABLE_STATUSES = List.of(REQUESTED, UNREACHABLE, STOPPED, DELETED_ON_PROVIDER_SIDE,
            DELETE_IN_PROGRESS, DELETE_COMPLETED, STALE);

    public static final Collection<Status> FREEIPA_VERTICALLY_NON_SCALABLE_STATUSES = List.of(DELETED_ON_PROVIDER_SIDE, DELETE_IN_PROGRESS, DELETE_COMPLETED);

    public static final Collection<Status> FREEIPA_CROSS_REALM_SETUP_FINISH_ENABLE_STATUSES = List.of(
            TRUST_SETUP_FINISH_REQUIRED,
            TRUST_SETUP_FINISH_FAILED);

    public static final Collection<Status> FREEIPA_CROSS_REALM_SETUP_ENABLE_STATUSES = List.of(AVAILABLE, TRUST_SETUP_FAILED);

    public static final Collection<Status> FREEIPA_STOPPABLE_STATUSES = List.of(AVAILABLE, STOP_FAILED, START_FAILED);

    public static final Collection<Status> FREEIPA_STARTABLE_STATUSES = List.of(STOPPED, STOP_FAILED, START_FAILED, STALE);

    public static final Collection<Status> FREEIPA_START_IN_PROGRESS_STATUSES = List.of(START_IN_PROGRESS);

    public static final Collection<Status> FREEIPA_STOP_IN_PROGRESS_STATUSES = List.of(STOP_IN_PROGRESS);

    public static final Collection<Status> FREEIPA_STOPPED_STATUSES = List.of(STOPPED);

    public static final Collection<Status> FREEIPA_CCM_UPGRADEABLE_STATUSES = List.of(AVAILABLE, UPGRADE_CCM_FAILED);

    public static final Collection<Status> FREEIPA_DEFAULT_OUTBOUND_UPGRADEABLE_STATUSES = List.of(AVAILABLE, UPGRADE_DEFAULT_OUTBOUND_FAILED);

    public static final Collection<Status> FREEIPA_PROXY_CONFIG_MODIFIABLE_STATUSES = List.of(AVAILABLE, MODIFY_PROXY_CONFIG_FAILED);

    private static final Map<Status, Status> IN_PROGRESS_TO_FINAL_STATUS_MAPPING = ImmutableMap.<Status, Status>builder()
            .put(REQUESTED, CREATE_FAILED)
            .put(UPDATE_REQUESTED, UPDATE_FAILED)
            .put(STOP_REQUESTED, STOP_FAILED)
            .put(START_REQUESTED, START_FAILED)
            .put(MODIFY_PROXY_CONFIG_REQUESTED, MODIFY_PROXY_CONFIG_FAILED)
            .put(MODIFY_PROXY_CONFIG_IN_PROGRESS, MODIFY_PROXY_CONFIG_FAILED)
            .put(CREATE_IN_PROGRESS, CREATE_FAILED)
            .put(UPDATE_IN_PROGRESS, UPDATE_FAILED)
            .put(DELETE_IN_PROGRESS, DELETE_FAILED)
            .put(STOP_IN_PROGRESS, STOP_FAILED)
            .put(START_IN_PROGRESS, START_FAILED)
            .put(DIAGNOSTICS_COLLECTION_IN_PROGRESS, AVAILABLE)
            .put(UPGRADE_CCM_REQUESTED, UPGRADE_CCM_FAILED)
            .put(UPGRADE_CCM_IN_PROGRESS, UPGRADE_CCM_FAILED)
            .put(UPGRADE_DEFAULT_OUTBOUND_REQUESTED, UPGRADE_DEFAULT_OUTBOUND_FAILED)
            .put(UPGRADE_DEFAULT_OUTBOUND_IN_PROGRESS, UPGRADE_DEFAULT_OUTBOUND_FAILED)
            .put(WAIT_FOR_SYNC, AVAILABLE)
            .put(REBUILD_IN_PROGRESS, REBUILD_FAILED)
            .put(TRUST_SETUP_IN_PROGRESS, TRUST_SETUP_FAILED)
            .put(TRUST_SETUP_FINISH_IN_PROGRESS, TRUST_SETUP_FINISH_FAILED)
            .put(CANCEL_TRUST_SETUP_IN_PROGRESS, CANCEL_TRUST_SETUP_FAILED)
            .build();

    private static final Collection<Status> FREEIPA_UNSCHEDULABLE_STATUSES = List.of(CREATE_FAILED, DELETE_IN_PROGRESS, DELETE_FAILED, DELETE_COMPLETED, STALE);

    public boolean isRemovableStatus() {
        return REMOVABLE_STATUSES.contains(this);
    }

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return DELETE_COMPLETED.equals(this);
    }

    public boolean isDeletedOnProviderSide() {
        return DELETED_ON_PROVIDER_SIDE.equals(this);
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

    public boolean isCcmUpgradeablePhase() {
        return FREEIPA_CCM_UPGRADEABLE_STATUSES.contains(this);
    }

    public boolean isDefaultOutboundUpgradeablePhase() {
        return FREEIPA_DEFAULT_OUTBOUND_UPGRADEABLE_STATUSES.contains(this);
    }

    public boolean isProxyConfigModifiablePhase() {
        return FREEIPA_PROXY_CONFIG_MODIFIABLE_STATUSES.contains(this);
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(this);
    }

    public boolean isUnschedulableState() {
        return FREEIPA_UNSCHEDULABLE_STATUSES.contains(this);
    }

    public boolean isVerticallyScalable() {
        return !FREEIPA_VERTICALLY_NON_SCALABLE_STATUSES.contains(this);
    }

    public boolean isCrossRealmFinishable() {
        return FREEIPA_CROSS_REALM_SETUP_FINISH_ENABLE_STATUSES.contains(this);
    }

    public boolean isCrossRealmPreparable() {
        return FREEIPA_CROSS_REALM_SETUP_ENABLE_STATUSES.contains(this);
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    public boolean isInProgress() {
        return switch (this) {
            case REQUESTED,
                 UPDATE_REQUESTED,
                 STOP_REQUESTED,
                 START_REQUESTED,
                 MODIFY_PROXY_CONFIG_REQUESTED,
                 CREATE_IN_PROGRESS,
                 UPDATE_IN_PROGRESS,
                 DELETE_IN_PROGRESS,
                 MODIFY_PROXY_CONFIG_IN_PROGRESS,
                 STOP_IN_PROGRESS,
                 DIAGNOSTICS_COLLECTION_IN_PROGRESS,
                 UPGRADE_CCM_REQUESTED,
                 UPGRADE_CCM_IN_PROGRESS,
                 UPGRADE_DEFAULT_OUTBOUND_REQUESTED,
                 UPGRADE_DEFAULT_OUTBOUND_IN_PROGRESS,
                 START_IN_PROGRESS,
                 WAIT_FOR_SYNC,
                 REBUILD_IN_PROGRESS,
                 TRUST_SETUP_IN_PROGRESS,
                 TRUST_SETUP_FINISH_IN_PROGRESS,
                 CANCEL_TRUST_SETUP_IN_PROGRESS -> true;
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
}