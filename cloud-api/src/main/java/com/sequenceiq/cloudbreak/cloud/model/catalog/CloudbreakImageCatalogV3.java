package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudbreakImageCatalogV3 {

    private final Images images;

    private final Versions versions;

    @JsonCreator
    public CloudbreakImageCatalogV3(
            @JsonProperty(value = "images", required = true) Images images,
            @JsonProperty(value = "versions", required = true) Versions versions) {
        this.images = images;
        this.versions = versions;
    }

    public Images getImages() {
        return images;
    }

    public Versions getVersions() {
        return versions;
    }
}
