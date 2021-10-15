package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CloudbreakImageCatalogV3 {

    private static final String VERSIONS = "versions";

    private static final String IMAGES = "images";

    private final Images images;

    private final Versions versions;

    @JsonCreator
    public CloudbreakImageCatalogV3(
            @JsonProperty(value = IMAGES, required = true) Images images,
            @JsonProperty(VERSIONS) Versions versions) {
        this.images = images;
        this.versions = versions;
    }

    @JsonProperty(IMAGES)
    public Images getImages() {
        return images;
    }

    @JsonProperty(VERSIONS)
    public Versions getVersions() {
        return versions;
    }
}
