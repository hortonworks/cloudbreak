package com.sequenceiq.freeipa.api.model.image;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FreeIpaVersions {

    private final List<String> versions;

    private final List<String> defaults;

    private final List<String> imageIds;

    @JsonCreator
    public FreeIpaVersions(
            @JsonProperty("versions") List<String> versions,
            @JsonProperty("defaults") List<String> defaults,
            @JsonProperty("images") List<String> imageIds) {
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
