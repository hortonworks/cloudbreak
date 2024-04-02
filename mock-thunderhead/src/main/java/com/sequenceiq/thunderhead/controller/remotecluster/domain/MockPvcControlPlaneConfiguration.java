package com.sequenceiq.thunderhead.controller.remotecluster.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MockPvcControlPlaneConfiguration {

    @JsonProperty
    private String pvcCrn;

    @JsonProperty
    private String baseUrl;

    @JsonProperty
    private String name;

    public String getPvcCrn() {
        return pvcCrn;
    }

    public void setPvcCrn(String pvcCrn) {
        this.pvcCrn = pvcCrn;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
