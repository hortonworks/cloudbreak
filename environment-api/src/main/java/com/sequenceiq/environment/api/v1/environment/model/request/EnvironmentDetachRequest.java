package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentDetachV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentDetachRequest extends EnvironmentBaseRequest {
    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentDetachRequest{}";
    }
}
