package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("FailureDetailsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureDetails {
    private String environment;

    private String message;

    public FailureDetails(String environment, String message) {
        this.environment = environment;
        this.message = message;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
