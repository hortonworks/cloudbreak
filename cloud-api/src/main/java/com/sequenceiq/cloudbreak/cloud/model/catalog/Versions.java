package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Versions {

    private final List<CloudbreakVersion> cloudbreakVersions;

    private final List<CloudbreakVersion> freeipaVersions;

    @JsonCreator
    public Versions(@JsonProperty(value = "cloudbreak") List<CloudbreakVersion> cloudbreakVersions,
                    @JsonProperty(value = "freeipa") List<CloudbreakVersion> freeipaVersions) {
        this.cloudbreakVersions = (cloudbreakVersions == null) ? Collections.emptyList() : cloudbreakVersions;
        this.freeipaVersions = (freeipaVersions == null) ? Collections.emptyList() : freeipaVersions;
    }

    public List<CloudbreakVersion> getCloudbreakVersions() {
        return cloudbreakVersions;
    }

    public List<CloudbreakVersion> getFreeipaVersions() {
        return freeipaVersions;
    }
}
