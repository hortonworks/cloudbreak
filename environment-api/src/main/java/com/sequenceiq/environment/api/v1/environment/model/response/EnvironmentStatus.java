package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentStatusV1")
public enum EnvironmentStatus {
    CREATION_INITIATED,
    NETWORK_CREATION_IN_PROGRESS,
    RDBMS_CREATION_IN_PROGRESS,
    FREEIPA_CREATION_IN_PROGRESS,
    AVAILABLE,
    ARCHIVED,
    CORRUPTED
}
