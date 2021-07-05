package com.sequenceiq.environment.api.v1.environment.model.request;

import com.sequenceiq.environment.api.v1.environment.model.base.SecurityAccessBase;

import io.swagger.annotations.ApiModel;

@ApiModel("SecurityAccessV1Request")
public class SecurityAccessRequest extends SecurityAccessBase {

    public static Builder<SecurityAccessRequest> builder() {
        return new Builder<>(SecurityAccessRequest.class);
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "SecurityAccessRequest{}";
    }
}
