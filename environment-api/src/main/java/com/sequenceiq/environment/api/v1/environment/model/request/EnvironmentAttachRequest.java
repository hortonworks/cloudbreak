package com.sequenceiq.environment.api.v1.environment.model.request;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentAttachV1Request")
public class EnvironmentAttachRequest extends EnvironmentBaseRequest {
    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentAttachRequest{}";
    }
}
