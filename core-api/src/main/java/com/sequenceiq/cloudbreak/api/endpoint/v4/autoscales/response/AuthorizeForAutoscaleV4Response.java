package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response;

import io.swagger.annotations.ApiModelProperty;

public class AuthorizeForAutoscaleV4Response {

    @ApiModelProperty
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
