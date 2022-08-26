package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaltPasswordStatusResponse {

    private SaltPasswordStatus status;

    public SaltPasswordStatus getStatus() {
        return status;
    }

    public void setStatus(SaltPasswordStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SaltPasswordStatusResponse{" +
                "status=" + status +
                '}';
    }
}
