package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkloadAnalyticsAttributesHolder implements Serializable {

    private String sdxId;

    private String sdxName;

    public String getSdxId() {
        return sdxId;
    }

    public void setSdxId(String sdxId) {
        this.sdxId = sdxId;
    }

    public String getSdxName() {
        return sdxName;
    }

    public void setSdxName(String sdxName) {
        this.sdxName = sdxName;
    }
}
