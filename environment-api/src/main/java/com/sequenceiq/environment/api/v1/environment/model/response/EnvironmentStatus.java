package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {

    CREATION_INITIATED("Creation Initiated"),
    DELETE_INITIATED("Deletion Initiated"),
    UPDATE_INITIATED("Update Initiated"),

    ENVIRONMENT_INITIALIZATION_IN_PROGRESS("Initialization in progress"),
    ENVIRONMENT_VALIDATION_IN_PROGRESS("Validation in progress"),
    PREREQUISITES_CREATE_IN_PROGRESS("Prerequisites creation in progress"),
    NETWORK_CREATION_IN_PROGRESS("Network creation in progress"),
    NETWORK_DELETE_IN_PROGRESS("Network deletion initiated"),

    RDBMS_DELETE_IN_PROGRESS("External Database deletion in progress"),

    FREEIPA_CREATION_IN_PROGRESS("Free IPA creation in progress"),
    FREEIPA_DELETE_IN_PROGRESS("Free IPA deletion in progress"),

    EXPERIENCE_DELETE_IN_PROGRESS("Experience deletion in progress"),

    CLUSTER_DEFINITION_CLEANUP_PROGRESS("Cleaning up cluster definitions"),

    UMS_RESOURCE_DELETE_IN_PROGRESS("User resources deletion in progress"),

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS("Deleting Role Mappings"),
    S3GUARD_TABLE_DELETE_IN_PROGRESS("Deleting DynamoDB table"),

    DATAHUB_CLUSTERS_DELETE_IN_PROGRESS("Deleting Data Hub clusters"),
    DATALAKE_CLUSTERS_DELETE_IN_PROGRESS("Deleting Data Lake cluster"),

    PUBLICKEY_CREATE_IN_PROGRESS("Creating SSH Public key"),
    PUBLICKEY_DELETE_IN_PROGRESS("Deleting SSH Public key"),

    AVAILABLE("Available"),
    ARCHIVED("Archived"),

    CREATE_FAILED("Creation failed"),
    DELETE_FAILED("Deletion failed"),
    UPDATE_FAILED("Update failed"),

    STOP_DATAHUB_STARTED("Stopping Data Hubs"),
    STOP_DATAHUB_FAILED("Failed to Stop Data Hubs"),
    STOP_DATALAKE_STARTED("Stopping Data Lake"),
    STOP_DATALAKE_FAILED("Failed to Stop Data Lake"),
    STOP_FREEIPA_STARTED("Stopping Free IPA"),
    STOP_FREEIPA_FAILED("Failed to Stop Free IPA"),

    ENV_STOPPED("Stopped"),

    START_DATAHUB_STARTED("Starting Data Hubs"),
    START_DATAHUB_FAILED("Failed to Start Data Hubs"),
    START_DATALAKE_STARTED("Starting Data Lake"),
    START_DATALAKE_FAILED("Failed to Start Data Lake"),
    START_FREEIPA_STARTED("Starting Free IPA"),
    START_FREEIPA_FAILED("Failed to Start Free IPA"),
    START_SYNCHRONIZE_USERS_STARTED("Starting to synchronize users"),
    START_SYNCHRONIZE_USERS_FAILED("Failed to synchronize users"),

    FREEIPA_DELETED_ON_PROVIDER_SIDE("Free IPA deleted on cloud provider side"),

    LOAD_BALANCER_ENV_UPDATE_STARTED("Starting load balancer update for environment"),
    LOAD_BALANCER_ENV_UPDATE_FAILED("Failed to update environment with load balancer"),
    LOAD_BALANCER_STACK_UPDATE_STARTED("Starting load balancer update for all data lakes and data hubs"),
    LOAD_BALANCER_STACK_UPDATE_FAILED("Failed to update data lakes and data hubs with load balancer");

    private static final Set<EnvironmentStatus> STARTABLE_STATUSES = Set.of(
            AVAILABLE,
            START_DATALAKE_STARTED,
            START_DATAHUB_STARTED,
            START_SYNCHRONIZE_USERS_STARTED,
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
            UPDATE_FAILED,
            START_FREEIPA_FAILED,
            START_DATAHUB_FAILED,
            START_DATALAKE_FAILED,
            START_SYNCHRONIZE_USERS_FAILED,
            STOP_DATAHUB_FAILED,
            STOP_DATALAKE_FAILED,
            STOP_FREEIPA_FAILED,
            FREEIPA_DELETED_ON_PROVIDER_SIDE
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
            START_SYNCHRONIZE_USERS_STARTED,
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
            PUBLICKEY_DELETE_IN_PROGRESS,
            EXPERIENCE_DELETE_IN_PROGRESS
    );

    private String description;

    EnvironmentStatus(String description) {
        this.description = description;
    }

    public static Set<EnvironmentStatus> startable() {
        return STARTABLE_STATUSES;
    }

    public static Set<EnvironmentStatus> stoppable() {
        return STOPPABLE_STATUSES;
    }

    public static Set<EnvironmentStatus> upscalable() {
        return UPSCALEABLE_STATUSES;
    }

    public String getDescription() {
        return description;
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
