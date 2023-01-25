package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PreparedImages {

    private List<String> preparedImages;

    @JsonCreator
    public PreparedImages(@JsonProperty("preparedImages") List<String> images) {
        this.preparedImages = images;
    }

    public List<String> getPreparedImages() {
        return preparedImages;
    }

    public void setPreparedImages(List<String> preparedImages) {
        this.preparedImages = preparedImages;
    }
}
