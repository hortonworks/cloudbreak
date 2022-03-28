package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentDeletionTypeV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public enum EnvironmentDeletionType {

    NONE, SIMPLE, FORCE
}
