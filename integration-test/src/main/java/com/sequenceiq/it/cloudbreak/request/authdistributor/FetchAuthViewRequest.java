package com.sequenceiq.it.cloudbreak.request.authdistributor;

public class FetchAuthViewRequest {

    private String environmentCrn;

    public FetchAuthViewRequest() {
    }

    public FetchAuthViewRequest(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
