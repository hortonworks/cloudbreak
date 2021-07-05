package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.v1.environment.model.base.SecurityAccessBase;

import io.swagger.annotations.ApiModel;

@ApiModel("SecurityAccessV1Response")
public class SecurityAccessResponse extends SecurityAccessBase {

    public static Builder<SecurityAccessResponse> builder() {
        return new Builder<>(SecurityAccessResponse.class);
    }

    @Override
    public String toString() {
        return super.toString() + "SecurityAccessResponse{}";
    }
}
