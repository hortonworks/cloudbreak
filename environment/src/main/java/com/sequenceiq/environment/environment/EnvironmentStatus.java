package com.sequenceiq.environment.environment;

import java.util.List;
import java.util.Set;

public enum EnvironmentStatus {

    CREATION_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATION_INITIATED),
    DELETE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED),
    UPDATE_INITIATED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_INITIATED),

    ENVIRONMENT_VALIDATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS),

    NETWORK_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS),
    NETWORK_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS),

    PUBLICKEY_CREATE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS),
    PUBLICKEY_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS),

    FREEIPA_CREATION_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS),
    FREEIPA_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS),

    RDBMS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS),

    CLUSTER_DEFINITION_DELETE_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS),

    UMS_RESOURCE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS),

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS),
    S3GUARD_TABLE_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS),

    DATAHUB_CLUSTERS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS),
    DATALAKE_CLUSTERS_DELETE_IN_PROGRESS(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS),

    AVAILABLE(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE),
    ARCHIVED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED),

    CREATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED),
    DELETE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED),
    UPDATE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UPDATE_FAILED),

    STOP_DATAHUB_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATAHUB_STARTED),
    STOP_DATAHUB_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATAHUB_FAILED),
    STOP_DATALAKE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATALAKE_STARTED),
    STOP_DATALAKE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_DATALAKE_FAILED),
    STOP_FREEIPA_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_FREEIPA_STARTED),
    STOP_FREEIPA_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.STOP_FREEIPA_FAILED),

    ENV_STOPPED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED),

    START_DATAHUB_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATAHUB_STARTED),
    START_DATAHUB_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATAHUB_FAILED),
    START_DATALAKE_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATALAKE_STARTED),
    START_DATALAKE_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATALAKE_FAILED),
    START_FREEIPA_STARTED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_FREEIPA_STARTED),
    START_FREEIPA_FAILED(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_FREEIPA_FAILED);

    public static final Set<EnvironmentStatus> AVAILABLE_STATUSES = Set.of(
            CREATION_INITIATED,
            UPDATE_INITIATED,
            NETWORK_CREATION_IN_PROGRESS,
            PUBLICKEY_CREATE_IN_PROGRESS,
            FREEIPA_CREATION_IN_PROGRESS,
            AVAILABLE);

    private final com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus;

    EnvironmentStatus(com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus getResponseStatus() {
        return responseStatus;
    }

    public boolean isDeleteInProgress() {
        return List.of(
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
                PUBLICKEY_DELETE_IN_PROGRESS
        ).contains(this);
    }

    public boolean isSuccessfullyDeleted() {
        return ARCHIVED == this;
    }
}
