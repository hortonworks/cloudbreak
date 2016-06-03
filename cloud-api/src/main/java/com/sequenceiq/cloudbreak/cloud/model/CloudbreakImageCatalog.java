package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakImageCatalog {

    @JsonProperty("cloudbreak")
    private List<AmbariCatalog> ambariVersions;

    public List<AmbariCatalog> getAmbariVersions() {
        return ambariVersions;
    }

    public void setAmbariVersions(List<AmbariCatalog> ambariVersions) {
        this.ambariVersions = ambariVersions;
    }
}
