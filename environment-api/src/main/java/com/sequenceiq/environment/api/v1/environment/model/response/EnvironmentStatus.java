package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {

    CREATION_INITIATED,
    DELETE_INITIATED,
    UPDATE_INITIATED,

    ENVIRONMENT_INITIALIZATION_IN_PROGRESS,
    ENVIRONMENT_VALIDATION_IN_PROGRESS,
    PREREQUISITES_CREATE_IN_PROGRESS,
    NETWORK_CREATION_IN_PROGRESS,
    NETWORK_DELETE_IN_PROGRESS,

    RDBMS_DELETE_IN_PROGRESS,

    FREEIPA_CREATION_IN_PROGRESS,
    FREEIPA_DELETE_IN_PROGRESS,

    CLUSTER_DEFINITION_CLEANUP_PROGRESS,

    UMS_RESOURCE_DELETE_IN_PROGRESS,
    PREREQUISITES_DELETE_IN_PROGRESS,

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
    S3GUARD_TABLE_DELETE_IN_PROGRESS,

    DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,
    DATALAKE_CLUSTERS_DELETE_IN_PROGRESS,

    PUBLICKEY_CREATE_IN_PROGRESS,
    PUBLICKEY_DELETE_IN_PROGRESS,

    AVAILABLE,
    ARCHIVED,

    CREATE_FAILED,
    DELETE_FAILED,
    UPDATE_FAILED,

    STOP_DATAHUB_STARTED,
    STOP_DATAHUB_FAILED,
    STOP_DATALAKE_STARTED,
    STOP_DATALAKE_FAILED,
    STOP_FREEIPA_STARTED,
    STOP_FREEIPA_FAILED,

    ENV_STOPPED,

    START_DATAHUB_STARTED,
    START_DATAHUB_FAILED,
    START_DATALAKE_STARTED,
    START_DATALAKE_FAILED,
    START_FREEIPA_STARTED,
    START_FREEIPA_FAILED,

    FREEIPA_DELETED_ON_PROVIDER_SIDE;

    private static final Set<EnvironmentStatus> STARTABLE_STATUSES = Set.of(
            AVAILABLE,
            START_DATALAKE_STARTED,
            START_DATAHUB_STARTED,
            START_FREEIPA_STARTED
    );

    private static final Set<EnvironmentStatus> STOPPABLE_STATUSES = Set.of(
            AVAILABLE,
            STOP_DATALAKE_STARTED,
            STOP_DATAHUB_STARTED,
            STOP_FREEIPA_STARTED
    );

    private static final Set<EnvironmentStatus> AVAILABLE_STATUSES = Set.of(
            AVAILABLE
    );

    private static final Set<EnvironmentStatus> UPSCALEABLE_STATUSES = Set.of(
            AVAILABLE
    );

    private static final Set<EnvironmentStatus> NETWORK_CREATION_FINISHED_STATUSES = Set.of(
            PUBLICKEY_CREATE_IN_PROGRESS,
            FREEIPA_CREATION_IN_PROGRESS,
            AVAILABLE
    );

    private static final Set<EnvironmentStatus> FAILED_STATUSES = Set.of(
            CREATE_FAILED,
            DELETE_FAILED,
            UPDATE_FAILED
    );

    private static final Set<EnvironmentStatus> STOP_IN_PROGRESS_OR_STOPPED_STATUSES = Set.of(
            STOP_DATAHUB_STARTED,
            STOP_DATALAKE_STARTED,
            STOP_FREEIPA_STARTED,
            ENV_STOPPED
    );

    private static final Set<EnvironmentStatus> START_IN_PROGRESS_STATUSES = Set.of(
            START_DATAHUB_STARTED,
            START_DATALAKE_STARTED,
            START_FREEIPA_STARTED
    );

    private static final Set<EnvironmentStatus> DELETE_IN_PROGRESS_STATUSES = Set.of(
            DELETE_INITIATED,
            NETWORK_DELETE_IN_PROGRESS,
            RDBMS_DELETE_IN_PROGRESS,
            FREEIPA_DELETE_IN_PROGRESS,
            CLUSTER_DEFINITION_CLEANUP_PROGRESS,
            UMS_RESOURCE_DELETE_IN_PROGRESS,
            IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
            S3GUARD_TABLE_DELETE_IN_PROGRESS,
            DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,
            DATALAKE_CLUSTERS_DELETE_IN_PROGRESS,
            PUBLICKEY_DELETE_IN_PROGRESS
    );

    public static Set<EnvironmentStatus> startable() {
        return STARTABLE_STATUSES;
    }

    public static Set<EnvironmentStatus> stoppable() {
        return STOPPABLE_STATUSES;
    }

    public static Set<EnvironmentStatus> upscalable() {
        return UPSCALEABLE_STATUSES;
    }

    public boolean isAvailable() {
        return AVAILABLE_STATUSES.contains(this);
    }

    public boolean isNetworkCreationFinished() {
        return NETWORK_CREATION_FINISHED_STATUSES.contains(this);
    }

    public boolean isFailed() {
        return FAILED_STATUSES.contains(this);
    }

    public boolean isStopInProgressOrStopped() {
        return STOP_IN_PROGRESS_OR_STOPPED_STATUSES.contains(this);
    }

    public boolean isStartInProgress() {
        return START_IN_PROGRESS_STATUSES.contains(this);
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS_STATUSES.contains(this);
    }
}
