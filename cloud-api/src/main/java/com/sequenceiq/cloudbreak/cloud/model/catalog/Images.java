package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Images {

    private List<Image> baseImages;

    private List<Image> hdpImages;

    private List<Image> hdfImages;

    @JsonCreator
    public Images(
            @JsonProperty(value = "base-images") List<Image> baseImages,
            @JsonProperty(value = "hdp-images") List<Image> hdpImages,
            @JsonProperty(value = "hdf-images") List<Image> hdfImages) {
        this.baseImages = (baseImages == null) ? emptyList() : baseImages;
        this.hdpImages = (hdpImages == null) ? emptyList() : hdpImages;
        this.hdfImages = (hdfImages == null) ? emptyList() : hdfImages;
    }

    public List<Image> getBaseImages() {
        return baseImages;
    }

    public List<Image> getHdpImages() {
        return hdpImages;
    }

    public List<Image> getHdfImages() {
        return hdfImages;
    }
}
