package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaVersions {

    private static final String IMAGES_PROPERTY = "images";

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<String> versions;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<String> defaults;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<String> imageIds;

    @JsonCreator
    public FreeIpaVersions(
            @JsonProperty("versions") List<String> versions,
            @JsonProperty("defaults") List<String> defaults,
            @JsonProperty(IMAGES_PROPERTY) List<String> imageIds) {
        this.versions = Optional.ofNullable(versions).orElse(List.of());
        this.defaults = Optional.ofNullable(defaults).orElse(List.of());
        this.imageIds = Optional.ofNullable(imageIds).orElse(List.of());
    }

    public List<String> getVersions() {
        return versions;
    }

    public List<String> getDefaults() {
        return defaults;
    }

    @JsonProperty(IMAGES_PROPERTY)
    public List<String> getImageIds() {
        return imageIds;
    }

    @Override
    public String toString() {
        return "FreeIpaVersions{"
                + "versions=" + versions
                + ", defaults=" + defaults
                + ", imageIds=" + imageIds
                + '}';
    }
}
