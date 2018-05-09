package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakVersion {

    private final List<String> versions;

    private final List<String> defaults;

    private final List<String> imageIds;

    @JsonCreator
    public CloudbreakVersion(
            @JsonProperty("versions") List<String> versions,
            @JsonProperty("defaults") List<String> defaults,
            @JsonProperty("images") List<String> imageIds) {
        this.versions = (versions == null) ? emptyList() : versions;
        this.defaults = (defaults == null) ? emptyList() : defaults;
        this.imageIds = (imageIds == null) ? emptyList() : imageIds;
    }

    public List<String> getVersions() {
        return versions;
    }

    public List<String> getDefaults() {
        return defaults;
    }

    public List<String> getImageIds() {
        return imageIds;
    }
}
