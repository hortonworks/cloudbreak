package com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteEnvironmentResponse {

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
        return "RemoteEnvironmentResponse{" +
                "crn='" + crn + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", status='" + status + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RemoteEnvironmentResponse that = (RemoteEnvironmentResponse) o;
        return Objects.equals(crn, that.crn)
                && Objects.equals(environmentName, that.environmentName)
                && Objects.equals(status, that.status)
                && Objects.equals(cloudPlatform, that.cloudPlatform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, environmentName, status, cloudPlatform);
    }
}
