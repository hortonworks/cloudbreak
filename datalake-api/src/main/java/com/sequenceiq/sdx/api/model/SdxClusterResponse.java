package com.sequenceiq.sdx.api.model;

public class SdxClusterResponse {

    private String crn;

    private String name;

    private SdxClusterStatusResponse status;

    private String environmentName;

    private String environmentCrn;

    public SdxClusterResponse() {
    }

    public SdxClusterResponse(String crn, String name, SdxClusterStatusResponse status, String environmentName, String environmentCrn) {
        this.crn = crn;
        this.name = name;
        this.status = status;
        this.environmentName = environmentName;
        this.environmentCrn = environmentCrn;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public SdxClusterStatusResponse getStatus() {
        return status;
    }

    public void setStatus(SdxClusterStatusResponse status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
