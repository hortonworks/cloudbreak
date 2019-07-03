package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("SuccessDetailsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuccessDetails {
    private String environment;

    public SuccessDetails(String environment) {
        this.environment = environment;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
