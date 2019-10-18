package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {

    CREATION_INITIATED,
    DELETE_INITIATED,
    UPDATE_INITIATED,

    NETWORK_CREATION_IN_PROGRESS,
    NETWORK_DELETE_IN_PROGRESS,

    RDBMS_DELETE_IN_PROGRESS,

    FREEIPA_CREATION_IN_PROGRESS,
    FREEIPA_DELETE_IN_PROGRESS,

    CLUSTER_DEFINITION_CLEANUP_PROGRESS,

    IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
    S3GUARD_TABLE_DELETE_IN_PROGRESS,

    AVAILABLE,
    ARCHIVED,

    CREATE_FAILED,
    DELETE_FAILED,
    UPDATE_FAILED;

    public boolean isAvailable() {
        return equals(AVAILABLE);
    }

    public boolean isFailed() {
        return equals(CREATE_FAILED) || equals(DELETE_FAILED) || equals(UPDATE_FAILED);
    }
}
