package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Versions {

    private final List<CloudbreakVersion> cloudbreakVersions;

    @JsonCreator
    public Versions(@JsonProperty(value = "cloudbreak", required = true) List<CloudbreakVersion> cloudbreakVersions) {
        this.cloudbreakVersions = (cloudbreakVersions == null) ? Collections.emptyList() : cloudbreakVersions;
    }

    public List<CloudbreakVersion> getCloudbreakVersions() {
        return cloudbreakVersions;
    }
}
