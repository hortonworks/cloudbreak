package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakVersion {

    private static final String VERSIONS = "versions";

    private static final String DEFAULTS = "defaults";

    private static final String IMAGES = "images";

    private final List<String> versions;

    private final List<String> defaults;

    private final List<String> imageIds;

    @JsonCreator
    public CloudbreakVersion(
            @JsonProperty(VERSIONS) List<String> versions,
            @JsonProperty(DEFAULTS) List<String> defaults,
            @JsonProperty(IMAGES) List<String> imageIds) {
        this.versions = (versions == null) ? emptyList() : versions;
        this.defaults = (defaults == null) ? emptyList() : defaults;
        this.imageIds = (imageIds == null) ? emptyList() : imageIds;
    }

    @JsonProperty(VERSIONS)
    public List<String> getVersions() {
        return versions;
    }

    @JsonProperty(DEFAULTS)
    public List<String> getDefaults() {
        return defaults;
    }

    @JsonProperty(IMAGES)
    public List<String> getImageIds() {
        return imageIds;
    }

    @Override
    public String toString() {
        return "CloudbreakVersion{"
                + "versions=" + versions
                + ", defaults=" + defaults
                + ", imageIds=" + imageIds
                + '}';
    }
}
