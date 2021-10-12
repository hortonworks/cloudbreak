package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Images {

    private static final String FREEIPA_IMAGES_PROPERTY = "freeipa-images";

    private final List<Image> images;

    @JsonCreator
    public Images(
            @JsonProperty(FREEIPA_IMAGES_PROPERTY) List<Image> images) {
        this.images = (images == null) ? emptyList() : images;
    }

    @JsonProperty(FREEIPA_IMAGES_PROPERTY)
    public List<Image> getFreeipaImages() {
        return images;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Images{");
        sb.append("images=").append(images);
        sb.append('}');
        return sb.toString();
    }
}
