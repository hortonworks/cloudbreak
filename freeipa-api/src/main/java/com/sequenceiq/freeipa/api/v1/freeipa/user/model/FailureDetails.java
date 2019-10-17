package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("FailureDetailsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailureDetails {
    private String environment;

    private String message;

    private Map<String, String> additionalDetails = new HashMap<>();

    public FailureDetails() {
    }

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

    public Map<String, String> getAdditionalDetails() {
        return additionalDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FailureDetails that = (FailureDetails) o;

        return Objects.equals(environment, that.environment)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, message);
    }

    @Override
    public String toString() {
        return "FailureDetails{"
                + "environment='" + environment + '\''
                + ", message='" + message + '\''
                + "additionalDetails='" + additionalDetails + '\''
                + '}';
    }
}
