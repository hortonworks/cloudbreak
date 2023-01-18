package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.environment.model.base.SecurityAccessBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SecurityAccessV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityAccessRequest extends SecurityAccessBase {

    public static Builder<SecurityAccessRequest> builder() {
        return new Builder<>(SecurityAccessRequest.class);
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "SecurityAccessRequest{}";
    }
}
