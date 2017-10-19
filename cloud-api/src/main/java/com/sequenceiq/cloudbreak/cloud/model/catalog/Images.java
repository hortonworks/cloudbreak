package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Images {

    @JsonProperty("base-images")
    private List<Image> baseImages;

    @JsonProperty("hdp-images")
    private List<Image> hdpImages;

    @JsonProperty("hdf-images")
    private List<Image> hdfImages;

    public List<Image> getBaseImages() {
        return baseImages;
    }

    public void setBaseImages(List<Image> baseImages) {
        this.baseImages = baseImages;
    }

    public List<Image> getHdpImages() {
        return hdpImages;
    }

    public void setHdpImages(List<Image> hdpImages) {
        this.hdpImages = hdpImages;
    }

    public List<Image> getHdfImages() {
        return hdfImages;
    }

    public void setHdfImages(List<Image> hdfImages) {
        this.hdfImages = hdfImages;
    }
}
