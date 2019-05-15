package com.sequenceiq.freeipa.api.model.image;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageCatalog {

    private final List<Image> images;

    @JsonCreator
    public ImageCatalog(List<Image> images) {
        this.images = images;
    }

    public List<Image> getImages() {
        return images;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageCatalog{");
        sb.append("images=").append(images);
        sb.append('}');
        return sb.toString();
    }
}
