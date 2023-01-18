package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentCrnV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentCrnResponse implements Serializable {

    private String environmentName;

    private String environmentCrn;

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String toString() {
        return "EnvironmentCrnResponse{" +
                "environmentName='" + environmentName + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
