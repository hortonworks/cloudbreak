package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel("SuccessDetailsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SuccessDetails {
    private String environment;

    public SuccessDetails() {
    }

    public SuccessDetails(String environment) {
        this.environment = environment;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SuccessDetails that = (SuccessDetails) o;

        return Objects.equals(environment, that.environment);

    }

    @Override
    public int hashCode() {
        return Objects.hash(environment);
    }

    @Override
    public String toString() {
        return "SuccessDetails{"
                + "environment='" + environment + '\''
                + '}';
    }
}
