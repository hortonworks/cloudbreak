package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {

    CREATION_INITIATED,
    DELETE_INITIATED,
    NETWORK_CREATION_IN_PROGRESS,
    NETWORK_DELETE_IN_PROGRESS,
    RDBMS_DELETE_IN_PROGRESS,
    FREEIPA_CREATION_IN_PROGRESS,
    FREEIPA_DELETE_IN_PROGRESS,
    AVAILABLE,
    ARCHIVED,
    CORRUPTED;

    public boolean isAvailable() {
        return equals(AVAILABLE);
    }

    public boolean isFailed() {
        return equals(CORRUPTED);
    }
}
