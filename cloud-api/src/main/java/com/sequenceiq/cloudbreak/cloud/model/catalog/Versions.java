package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Versions {

    private static final String CLOUDBREAK = "cloudbreak";

    private static final String FREEIPA = "freeipa";

    private final List<CloudbreakVersion> cloudbreakVersions;

    private final List<CloudbreakVersion> freeipaVersions;

    @JsonCreator
    public Versions(@JsonProperty(CLOUDBREAK) List<CloudbreakVersion> cloudbreakVersions,
                    @JsonProperty(FREEIPA) List<CloudbreakVersion> freeipaVersions) {
        this.cloudbreakVersions = (cloudbreakVersions == null) ? Collections.emptyList() : cloudbreakVersions;
        this.freeipaVersions = (freeipaVersions == null) ? Collections.emptyList() : freeipaVersions;
    }

    @JsonProperty(CLOUDBREAK)
    public List<CloudbreakVersion> getCloudbreakVersions() {
        return cloudbreakVersions;
    }

    @JsonProperty(FREEIPA)
    public List<CloudbreakVersion> getFreeipaVersions() {
        return freeipaVersions;
    }
}
