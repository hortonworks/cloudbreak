package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariCatalog {

    @JsonProperty("ambari")
    private AmbariInfo ambariInfo;

    public AmbariInfo getAmbariInfo() {
        return ambariInfo;
    }

    public void setAmbariInfo(AmbariInfo ambariInfo) {
        this.ambariInfo = ambariInfo;
    }
}
