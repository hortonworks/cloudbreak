package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentStatusV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public enum EnvironmentStatus {

    CREATION_INITIATED("Creation Initiated"),
    DELETE_INITIATED("Deletion Initiated"),
    UPDATE_INITIATED("Update Initiated"),

    ENVIRONMENT_INITIALIZATION_IN_PROGRESS("Initialization in progress"),
    ENVIRONMENT_VALIDATION_IN_PROGRESS("Validation in progress"),

    STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_IN_PROGRESS("Storage consumption collection scheduling in progress"),
    STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_IN_PROGRESS("Storage consumption collection unscheduling in progress"),

    PREREQUISITES_CREATE_IN_PROGRESS("Prerequisites creation in progress"),

    NETWORK_CREATION_IN_PROGRESS("Network creation in progress"),
    NETWORK_DELETE_IN_PROGRESS("Network deletion initiated"),

    RDBMS_DELETE_IN_PROGRESS("External Database deletion in progress"),

    FREEIPA_CREATION_IN_PROGRESS("FreeIPA creation in progress"),
    FREEIPA_DELETE_IN_PROGRESS("FreeIPA deletion in progress"),

    COMPUTE_CLUSTER_CREATION_IN_PROGRESS("Compute cluster creation in progress"),
    COMPUTE_CLUSTER_REINITIALIZATION_IN_PROGRESS("Compute cluster reinitialization in progress"),

    EXPERIENCE_DELETE_IN_PROGRESS("Experience deletion in progress"),

    CLUSTER_DEFINITION_CLEANUP_PROGRESS("Cleaning up cluster definitions"),

    UMS_RESOURCE_DELETE_IN_PROGRESS("User resources deletion in progress"),

    EVENT_CLEANUP_IN_PROGRESS("Environment event cleanup in progress"),

    DISTRIBUTION_LIST_CLEANUP_IN_PROGRESS("Deleting the Distribution list for the environment"),

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS("Deleting Role Mappings"),
    S3GUARD_TABLE_DELETE_IN_PROGRESS("Deleting DynamoDB table"),

    DATAHUB_CLUSTERS_DELETE_IN_PROGRESS("Deleting Data Hub clusters"),
    DATALAKE_CLUSTERS_DELETE_IN_PROGRESS("Deleting Data Lake cluster"),
    COMPUTE_CLUSTERS_DELETE_IN_PROGRESS("Deleting Compute clusters"),

    PUBLICKEY_CREATE_IN_PROGRESS("Creating SSH Public key"),
    PUBLICKEY_DELETE_IN_PROGRESS("Deleting SSH Public key"),

    ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS("Initializing Resource Encryption settings for environment"),
    ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS("Deleting Resource Encryption settings of environment"),
    ENVIRONMENT_ENCRYPTION_RESOURCES_INITIALIZED("Created Resource Encryption settings for environment"),
    ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED("Deleted Resource Encryption settings for environment"),

    AVAILABLE("Available"),
    ARCHIVED("Archived"),

    CREATE_FAILED("Creation failed"),
    DELETE_FAILED("Deletion failed"),
    UPDATE_FAILED("Update failed"),

    STOP_DATAHUB_STARTED("Stopping Data Hubs"),
    STOP_DATAHUB_FAILED("Failed to Stop Data Hubs"),
    STOP_DATALAKE_STARTED("Stopping Data Lake"),
    STOP_DATALAKE_FAILED("Failed to Stop Data Lake"),
    STOP_FREEIPA_STARTED("Stopping FreeIPA"),
    STOP_FREEIPA_FAILED("Failed to Stop FreeIPA"),

    ENV_STOPPED("Stopped"),

    START_DATAHUB_STARTED("Starting Data Hubs"),
    START_DATAHUB_FAILED("Failed to Start Data Hubs"),
    START_DATALAKE_STARTED("Starting Data Lake"),
    START_DATALAKE_FAILED("Failed to Start Data Lake"),
    START_FREEIPA_STARTED("Starting FreeIPA"),
    START_FREEIPA_FAILED("Failed to Start FreeIPA"),
    START_SYNCHRONIZE_USERS_STARTED("Starting to synchronize users"),
    START_SYNCHRONIZE_USERS_FAILED("Failed to synchronize users"),

    FREEIPA_DELETED_ON_PROVIDER_SIDE("FreeIPA deleted on cloud provider side"),
    FREEIPA_UNHEALTHY("FreeIPA is unhealthy"),
    FREEIPA_UNREACHABLE("FreeIPA is unreachable"),
    FREEIPA_REBUILD_IN_PROGRESS("Rebuilding FreeIPA"),
    FREEIPA_REBUILD_FAILED("Rebuilding FreeIPA failed"),
    FREEIPA_STALE("FreeIPA is stale"),

    LOAD_BALANCER_ENV_UPDATE_STARTED("Starting load balancer update for environment"),
    LOAD_BALANCER_ENV_UPDATE_FAILED("Failed to update environment with load balancer"),
    LOAD_BALANCER_STACK_UPDATE_STARTED("Starting load balancer update for all data lakes and data hubs"),
    LOAD_BALANCER_STACK_UPDATE_FAILED("Failed to update data lakes and data hubs with load balancer"),

    UPGRADE_CCM_VALIDATION_IN_PROGRESS("Validation for CCM upgrade"),
    UPGRADE_CCM_VALIDATION_FAILED("Validation for CCM upgrade failed"),
    UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS("Upgrading CCM on FreeIPA"),
    UPGRADE_CCM_ON_FREEIPA_FAILED("Upgrading CCM on FreeIPA failed"),
    UPGRADE_CCM_TUNNEL_UPDATE_IN_PROGRESS("Updating CCM tunnel type of the environment"),
    UPGRADE_CCM_TUNNEL_UPDATE_FAILED("Updating CCM tunnel type of the environment failed"),
    UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS("Upgrading CCM on Data Lake"),
    UPGRADE_CCM_ON_DATALAKE_FAILED("Upgrading CCM on Data Lake failed"),
    UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS("Upgrading CCM on Data Hub clusters"),
    UPGRADE_CCM_ON_DATAHUB_FAILED("Upgrading CCM on Data Hub clusters failed"),
    UPGRADE_CCM_FAILED("Upgrade CCM failed"),
    UPGRADE_CCM_ROLLING_BACK("Rolling back failed CCM upgrade"),

    VERTICAL_SCALE_VALIDATION_IN_PROGRESS("Validation for Vertical Scale"),
    VERTICAL_SCALE_VALIDATION_FAILED("Validation for Vertical Scale failed"),
    VERTICAL_SCALE_ON_FREEIPA_IN_PROGRESS("Vertical Scale on FreeIPA"),
    VERTICAL_SCALE_ON_FREEIPA_FAILED("Vertical Scale on FreeIPA failed"),
    VERTICAL_SCALE_FAILED("Vertical Scale failed"),

    TRUST_SETUP_REQUIRED("Trust Setup Required"),
    TRUST_BROKEN("Trust broken"),

    TRUST_SETUP_VALIDATION_IN_PROGRESS("Cross Realm Trust setup validation in progress"),
    TRUST_SETUP_VALIDATION_FAILED("Cross Realm Trust setup validation failed"),
    TRUST_SETUP_IN_PROGRESS("Cross Realm Trust setup in progress"),
    TRUST_SETUP_FAILED("Cross Realm Trust setup failed"),
    TRUST_SETUP_FINISH_REQUIRED("Cross Realm Trust setup finish required"),

    TRUST_CANCEL_VALIDATION_IN_PROGRESS("Cross Realm Trust cancel validation in progress"),
    TRUST_CANCEL_VALIDATION_FAILED("Cross Realm Trust cancel validation failed"),
    TRUST_CANCEL_IN_PROGRESS("Cross Realm Trust cancel in progress"),
    TRUST_CANCEL_FAILED("Cross Realm Trust cancel failed"),

    TRUST_SETUP_FINISH_VALIDATION_IN_PROGRESS("Cross Realm Trust setup finish validation in progress"),
    TRUST_SETUP_FINISH_VALIDATION_FAILED("Cross Realm Trust setup finish validation failed"),
    TRUST_SETUP_FINISH_IN_PROGRESS("Cross Realm Trust setup finish in progress"),
    TRUST_SETUP_FINISH_FAILED("Cross Realm Trust setup finish failed"),

    TRUST_REPAIR_VALIDATION_IN_PROGRESS("Cross Realm Trust repair validation in progress"),
    TRUST_REPAIR_VALIDATION_FAILED("Cross Realm Trust repair validation failed"),
    TRUST_REPAIR_IN_PROGRESS("Cross Realm Trust repair in progress"),
    TRUST_REPAIR_FAILED("Cross Realm Trust repair failed"),

    ENABLE_SELINUX_VALIDATION_IN_PROGRESS("Validation for Enable SeLinux"),
    ENABLE_SELINUX_VALIDATION_FAILED("Validation for Enable SeLinux failed"),
    ENABLE_SELINUX_ON_FREEIPA_IN_PROGRESS("Enable SeLinux on FreeIPA"),
    ENABLE_SELINUX_ON_FREEIPA_FAILED("Enable SeLinux on FreeIPA failed"),
    ENABLE_SELINUX_FAILED("Enable SeLinux failed"),

    PROXY_CONFIG_MODIFICATION_IN_PROGRESS("Starting proxy configuration modification"),
    PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS("Modifying proxy configuration on FreeIPA"),
    PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED("Failed to modify proxy configuration on FreeIPA"),
    PROXY_CONFIG_MODIFICATION_ON_DATALAKE_IN_PROGRESS("Modifying proxy configuration on DataLake"),
    PROXY_CONFIG_MODIFICATION_ON_DATALAKE_FAILED("Failed to modify proxy configuration on DataLake"),
    PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_IN_PROGRESS("Modifying proxy configuration on DataHubs"),
    PROXY_CONFIG_MODIFICATION_ON_DATAHUBS_FAILED("Failed to modify proxy configuration on DataHubs"),
    PROXY_CONFIG_MODIFICATION_FAILED("Proxy configuration modification failed"),
    FREEIPA_PREPARE_CROSS_REALM_TRUST_IN_PROGRESS("Preparing cross-realm trust on FreeIPA"),
    FREEIPA_PREPARE_CROSS_REALM_TRUST_PENDING("Cross-realm trust set-up on FreeIPA pending"),
    FREEIPA_PREPARE_CROSS_REALM_TRUST_FAILED("Failed to prepare cross-realm trust on FreeIPA"),
    FREEIPA_FINISH_CROSS_REALM_TRUST_IN_PROGRESS("Finishing set up of cross-realm trust on FreeIPA"),
    FREEIPA_FINISH_CROSS_REALM_TRUST_FAILED("Failed to finish setting up cross-realm trust on FreeIPA"),
    FREEIPA_FINISH_CROSS_REALM_TRUST_SUCCESSFUL("Setting up cross-realm trust on FreeIPA finished successfully"),
    UPGRADE_DEFAULT_OUTBOUND_ON_FREEIPA_IN_PROGRESS("Upgrading Default Outbound on FreeIPA"),
    UPGRADE_DEFAULT_OUTBOUND_ON_FREEIPA_FAILED("Failed to upgrade Default Outbound on FreeIPA"),;

    private static final Set<EnvironmentStatus> STARTABLE_STATUSES = Set.of(
            AVAILABLE,
            START_DATALAKE_STARTED,
            START_DATAHUB_STARTED,
            START_SYNCHRONIZE_USERS_STARTED,
            START_FREEIPA_STARTED,
            FREEIPA_STALE,
            TRUST_BROKEN
    );

    private static final Set<EnvironmentStatus> STOPPABLE_STATUSES = Set.of(
            AVAILABLE,
            STOP_DATALAKE_STARTED,
            STOP_DATAHUB_STARTED,
            STOP_FREEIPA_STARTED,
            TRUST_BROKEN
    );

    private static final Set<EnvironmentStatus> AVAILABLE_STATUSES = Set.of(
            AVAILABLE
    );

    private static final Set<EnvironmentStatus> UPSCALEABLE_STATUSES = Set.of(
            AVAILABLE
    );

    private static final Set<EnvironmentStatus> NETWORK_CREATION_FINISHED_STATUSES = Set.of(
            PUBLICKEY_CREATE_IN_PROGRESS,
            ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS,
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
            FREEIPA_DELETED_ON_PROVIDER_SIDE,
            FREEIPA_UNHEALTHY,
            FREEIPA_UNREACHABLE,
            UPGRADE_CCM_VALIDATION_FAILED,
            UPGRADE_CCM_ON_FREEIPA_FAILED,
            UPGRADE_CCM_ON_DATALAKE_FAILED,
            UPGRADE_CCM_ON_DATAHUB_FAILED,
            UPGRADE_CCM_FAILED
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
            ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS,
            FREEIPA_DELETE_IN_PROGRESS,
            CLUSTER_DEFINITION_CLEANUP_PROGRESS,
            UMS_RESOURCE_DELETE_IN_PROGRESS,
            IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
            S3GUARD_TABLE_DELETE_IN_PROGRESS,
            DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,
            DATALAKE_CLUSTERS_DELETE_IN_PROGRESS,
            PUBLICKEY_DELETE_IN_PROGRESS,
            EXPERIENCE_DELETE_IN_PROGRESS,
            COMPUTE_CLUSTERS_DELETE_IN_PROGRESS
    );

    private final String description;

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

    public boolean isComputeClusterCreationInProgress() {
        return COMPUTE_CLUSTER_CREATION_IN_PROGRESS.equals(this) || COMPUTE_CLUSTER_REINITIALIZATION_IN_PROGRESS.equals(this);
    }
}
