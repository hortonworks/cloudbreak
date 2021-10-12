package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageCatalog {

    private final Images images;

    private final Versions versions;

    @JsonCreator
    public ImageCatalog(@JsonProperty(value = "images", required = true) Images images, @JsonProperty(value = "versions") Versions versions) {
        this.images = images;
        this.versions = Optional.ofNullable(versions).orElse(new Versions(List.of()));
    }

    public Images getImages() {
        return images;
    }

    public Versions getVersions() {
        return versions;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageCatalog{");
        sb.append("images=").append(images);
        sb.append(", versions=").append(versions);
        sb.append('}');
        return sb.toString();
    }
}
