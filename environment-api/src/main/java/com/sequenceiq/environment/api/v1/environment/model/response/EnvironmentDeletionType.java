package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentDeletionTypeV1")
public enum EnvironmentDeletionType {

    NONE, SIMPLE, FORCE
}
