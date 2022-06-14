package com.sequenceiq.environment.environment;

import java.util.Set;

public enum EnvironmentStatus {

    // Environment creation request was registered in the Database and we are starting the creation flow
    CREATION_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATION_INITIATED),
    // Environment deletion request was registered and we are starting the deletion flow
    DELETE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED),
    // Environment update requested and starting the update flow (network update, load balancer update, ssh key update)
    UPDATE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_INITIATED),

    // Setting up the regions and network metadata (public/private and cidr)
    ENVIRONMENT_INITIALIZATION_IN_PROGRESS(
            com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENVIRONMENT_INITIALIZATION_IN_PROGRESS),
    // Setting up the regions and network metadata (public/private and cidr)
    ENVIRONMENT_VALIDATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS),

    STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_IN_PROGRESS(
            com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_IN_PROGRESS),
    STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_IN_PROGRESS(
            com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_IN_PROGRESS),

    // If the user choosing create new option for the network then we create the network on provider side
    NETWORK_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS),
    // If the user choosing create new option for the network then we delete on provider side
    NETWORK_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS),

    // If the user choosing create new option for the ssh key then we create the ssh key on provider side
    PUBLICKEY_CREATE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS),
    // If the user choosing create new option for the ssh key then we delete the ssh key on provider side
    PUBLICKEY_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS),

    // If the user chooses to encrypt resources with CMK then we create required encryption resources which will be used to encrypt other resources
    ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus
        .ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS),
    // If the user chooses to encrypt resources with CMK then we delete encryption resources which are used to encrypt other resources
    ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus
        .ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS),
    // If the user chooses to encrypt resources with CMK then we create required encryption resources in cloud which will be used to
    // encrypt other resources
    ENVIRONMENT_ENCRYPTION_RESOURCES_INITIALIZED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus
            .ENVIRONMENT_ENCRYPTION_RESOURCES_INITIALIZED),
    // If the user chooses to encrypt resources with CMK then we delete encryption resources which are used to encrypt other resources
    ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus
            .ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED),

    // Creating the FreeIPA resource for the environment
    FREEIPA_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS),
    // Deleting the FreeIPA resource for the environment
    FREEIPA_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS),

    // Deleting all the attached DWX/ML ..etc cluster (currently only DWX and Monitoring)
    EXPERIENCE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.EXPERIENCE_DELETE_IN_PROGRESS),

    // Deleting all the provisioned RDS which are related to the environment
    RDBMS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS),

    // Deleting all the cluster definition which is created by the user for the environment
    CLUSTER_DEFINITION_DELETE_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS),

    // Deleting all the related UMS resource for the environment
    UMS_RESOURCE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS),

    // Deleting all the ID broker mapping for the environment
    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS),
    // Deleting all the Dynamo DB table for the environment (If user choosing create new)
    S3GUARD_TABLE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS),

    // Deleting all the attached Data Hub Cluster
    DATAHUB_CLUSTERS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS),
    // Deleting all the Data Lake cluster (currently only 1)
    DATALAKE_CLUSTERS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS),

    // Environment is Available (ready to use)
    AVAILABLE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE),
    // Environment is Deleted (not shown on the UI)
    ARCHIVED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED),

    // Environment creation failed (Detailed message in the statusReason)
    CREATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED),
    // Environment deletion failed (Detailed message in the statusReason)
    DELETE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED),
    // Environment update failed (Detailed message in the statusReason)
    UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_FAILED),

    // Stopping all the Data Hub cluster in an Environment
    STOP_DATAHUB_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATAHUB_STARTED),
    // Stopping all the Data Hub cluster failed in an Environment (Detailed message in the statusReason)
    STOP_DATAHUB_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATAHUB_FAILED),
    // Stopping all the Data Lake cluster in an Environment (currently 1)
    STOP_DATALAKE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATALAKE_STARTED),
    // Stopping all the Data Lake cluster failed in an Environment (currently 1 / Detailed message in the statusReason)
    STOP_DATALAKE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATALAKE_FAILED),
    // Stopping the FreeIPA instances in an Environment
    STOP_FREEIPA_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_FREEIPA_STARTED),
    // Stopping the FreeIPA instances failed in an Environment (Detailed message in the statusReason)
    STOP_FREEIPA_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_FREEIPA_FAILED),

    // Environment successfully stopped
    ENV_STOPPED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED),

    // Starting all the Data Hub cluster in an Environment
    START_DATAHUB_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATAHUB_STARTED),
    // Starting all the Data Hub cluster failed in an Environment (Detailed message in the statusReason)
    START_DATAHUB_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATAHUB_FAILED),
    // Starting all the Data Lake cluster in an Environment (currently 1)
    START_DATALAKE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATALAKE_STARTED),
    // Starting all the Data Lake cluster failed in an Environment (currently 1 / Detailed message in the statusReason)
    START_DATALAKE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATALAKE_FAILED),
    // Starting the FreeIPA instances in an Environment
    START_FREEIPA_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_FREEIPA_STARTED),
    // Starting the FreeIPA instances failed in an Environment (Detailed message in the statusReason)
    START_FREEIPA_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_FREEIPA_FAILED),
    // Starting a usersync for all the cluster in an Environment
    START_SYNCHRONIZE_USERS_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_SYNCHRONIZE_USERS_STARTED),
    // Starting a usersync for all the cluster failed in an Environment (Detailed message in the statusReason)
    START_SYNCHRONIZE_USERS_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_SYNCHRONIZE_USERS_FAILED),

    // The FreeIPA instance deleted on provider side
    FREEIPA_DELETED_ON_PROVIDER_SIDE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE),

    // Start updating the LoadBalancer on Data Lake in an Environment
    LOAD_BALANCER_ENV_UPDATE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.LOAD_BALANCER_ENV_UPDATE_STARTED),
    // Failed to updating the LoadBalancer on Data Lake in an Environment (Detailed message in the statusReason)
    LOAD_BALANCER_ENV_UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.LOAD_BALANCER_ENV_UPDATE_FAILED),
    // Start updating the LoadBalancer on Data Hubs in an Environment
    LOAD_BALANCER_STACK_UPDATE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.LOAD_BALANCER_STACK_UPDATE_STARTED),
    // Failed to updating the LoadBalancer on Data Hubs in an Environment (Detailed message in the statusReason)
    LOAD_BALANCER_STACK_UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.LOAD_BALANCER_STACK_UPDATE_FAILED),

    // Upgrade CCM
    UPGRADE_CCM_VALIDATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS),
    UPGRADE_CCM_VALIDATION_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_VALIDATION_FAILED),
    UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS),
    UPGRADE_CCM_ON_FREEIPA_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED),
    UPGRADE_CCM_TUNNEL_UPDATE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_TUNNEL_UPDATE_IN_PROGRESS),
    UPGRADE_CCM_TUNNEL_UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_TUNNEL_UPDATE_FAILED),
    UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS),
    UPGRADE_CCM_ON_DATALAKE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED),
    UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS),
    UPGRADE_CCM_ON_DATAHUB_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED),
    UPGRADE_CCM_ROLLING_BACK(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_ROLLING_BACK),
    UPGRADE_CCM_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPGRADE_CCM_FAILED);

    public static final Set<EnvironmentStatus> AVAILABLE_STATUSES = Set.of(
            CREATION_INITIATED,
            UPDATE_INITIATED,
            NETWORK_CREATION_IN_PROGRESS,
            PUBLICKEY_CREATE_IN_PROGRESS,
            ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS,
            FREEIPA_CREATION_IN_PROGRESS,
            AVAILABLE);

    public static final Set<EnvironmentStatus> DELETE_IN_PROGRESS_STATES = Set.of(
            NETWORK_DELETE_IN_PROGRESS,
            FREEIPA_DELETE_IN_PROGRESS,
            RDBMS_DELETE_IN_PROGRESS,
            IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
            S3GUARD_TABLE_DELETE_IN_PROGRESS,
            CLUSTER_DEFINITION_DELETE_PROGRESS,
            UMS_RESOURCE_DELETE_IN_PROGRESS,
            DELETE_INITIATED,
            DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,
            DATALAKE_CLUSTERS_DELETE_IN_PROGRESS,
            PUBLICKEY_DELETE_IN_PROGRESS,
            EXPERIENCE_DELETE_IN_PROGRESS,
            ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS,
            ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED
    );

    public static final Set<EnvironmentStatus> CCM_UPGRADEABLE_STATES = Set.of(
            AVAILABLE,
            UPGRADE_CCM_VALIDATION_FAILED,
            UPGRADE_CCM_ON_FREEIPA_FAILED,
            UPGRADE_CCM_TUNNEL_UPDATE_FAILED,
            UPGRADE_CCM_ON_DATALAKE_FAILED,
            UPGRADE_CCM_ON_DATAHUB_FAILED,
            UPGRADE_CCM_FAILED
    );

    private final com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus;

    EnvironmentStatus(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus getResponseStatus() {
        return responseStatus;
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS_STATES.contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return ARCHIVED == this;
    }

    public boolean isCcmUpgradeablePhase() {
        return CCM_UPGRADEABLE_STATES.contains(this);
    }
}
