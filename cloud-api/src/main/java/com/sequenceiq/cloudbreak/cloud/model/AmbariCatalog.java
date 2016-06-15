package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariCatalog implements Versioned {

    @JsonProperty("ambari")
    private AmbariInfo ambariInfo;

    public AmbariInfo getAmbariInfo() {
        return ambariInfo;
    }

    public void setAmbariInfo(AmbariInfo ambariInfo) {
        this.ambariInfo = ambariInfo;
    }

    @Override
    public String getVersion() {
        if (ambariInfo == null) {
            return null;
        }
        return ambariInfo.getVersion();
    }

    @Override
    public String toString() {
        return "AmbariCatalog{version=" + getVersion() + "'}";
    }
}
