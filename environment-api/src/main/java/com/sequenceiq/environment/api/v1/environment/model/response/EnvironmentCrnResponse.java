package com.sequenceiq.environment.api.v1.environment.model.response;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentCrnV1Response")
public class EnvironmentCrnResponse {

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
}
