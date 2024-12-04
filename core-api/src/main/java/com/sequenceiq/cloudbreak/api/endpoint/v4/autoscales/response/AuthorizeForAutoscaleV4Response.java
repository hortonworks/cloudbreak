package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthorizeForAutoscaleV4Response {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
