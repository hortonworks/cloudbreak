package com.sequenceiq.sdx.api.model;

public class SdxClusterResponse {

    private String sdxCrn;

    private String sdxName;

    private SdxClusterStatusResponse status;

    private String environmentName;

    private String environmentCrn;

    public String getSdxCrn() {
        return sdxCrn;
    }

    public void setSdxCrn(String sdxCrn) {
        this.sdxCrn = sdxCrn;
    }

    public SdxClusterStatusResponse getStatus() {
        return status;
    }

    public void setStatus(SdxClusterStatusResponse status) {
        this.status = status;
    }

    public String getSdxName() {
        return sdxName;
    }

    public void setSdxName(String sdxName) {
        this.sdxName = sdxName;
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
