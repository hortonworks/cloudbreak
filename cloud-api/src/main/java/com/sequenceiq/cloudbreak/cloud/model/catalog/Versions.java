package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Versions {

    @JsonProperty("cloudbreak")
    private List<CloudbreakVersion> cloudbreakVersions;

    public List<CloudbreakVersion> getCloudbreakVersions() {
        return cloudbreakVersions;
    }

    public void setCloudbreakVersions(List<CloudbreakVersion> cloudbreakVersions) {
        this.cloudbreakVersions = cloudbreakVersions;
    }
}
