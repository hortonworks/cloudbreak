package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {

    CREATION_INITIATED,
    DELETE_INITIATED,
    UPDATE_INITIATED,

    ENVIRONMENT_VALIDATION_IN_PROGRESS,
    NETWORK_CREATION_IN_PROGRESS,
    NETWORK_DELETE_IN_PROGRESS,

    RDBMS_DELETE_IN_PROGRESS,

    FREEIPA_CREATION_IN_PROGRESS,
    FREEIPA_DELETE_IN_PROGRESS,

    CLUSTER_DEFINITION_CLEANUP_PROGRESS,

    UMS_RESOURCE_DELETE_IN_PROGRESS,

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

    public static Set<EnvironmentStatus> startable() {
        return Set.of(AVAILABLE, START_DATALAKE_STARTED, START_DATAHUB_STARTED, START_FREEIPA_STARTED);
    }

    public static Set<EnvironmentStatus> stoppable() {
        return Set.of(AVAILABLE, STOP_DATALAKE_STARTED, STOP_DATAHUB_STARTED, STOP_FREEIPA_STARTED);
    }

    public static Set<EnvironmentStatus> upscalable() {
        return Set.of(AVAILABLE);
    }

    public boolean isAvailable() {
        return equals(AVAILABLE);
    }

    public boolean isNetworkCreationFinished() {
        return equals(PUBLICKEY_CREATE_IN_PROGRESS) || equals(FREEIPA_CREATION_IN_PROGRESS) || equals(AVAILABLE);
    }

    public boolean isFailed() {
        return equals(CREATE_FAILED) || equals(DELETE_FAILED) || equals(UPDATE_FAILED);
    }
}
