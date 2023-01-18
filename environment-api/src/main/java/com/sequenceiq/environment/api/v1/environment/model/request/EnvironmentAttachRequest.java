package com.sequenceiq.environment.api.v1.environment.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentAttachV1Request")
public class EnvironmentAttachRequest extends EnvironmentBaseRequest {
    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentAttachRequest{}";
    }
}
