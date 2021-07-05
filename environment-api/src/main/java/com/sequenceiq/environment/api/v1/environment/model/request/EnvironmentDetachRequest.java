package com.sequenceiq.environment.api.v1.environment.model.request;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentDetachV1Request")
public class EnvironmentDetachRequest extends EnvironmentBaseRequest {
    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentDetachRequest{}";
    }
}
