package com.sequenceiq.thunderhead.controller.remotecluster.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MockRemoteEnvironmentResponse {

    @JsonProperty
    private String crn;

    @JsonProperty
    private String environmentName;

    @JsonProperty
    private String status;

    @JsonProperty
    private String cloudPlatform;

    public String getCrn() {
        return crn;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getStatus() {
        return status;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    @Override
    public String toString() {
        return "MockRemoteEnvironmentResponse{" +
                "crn='" + crn + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", status='" + status + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                '}';
    }
}
