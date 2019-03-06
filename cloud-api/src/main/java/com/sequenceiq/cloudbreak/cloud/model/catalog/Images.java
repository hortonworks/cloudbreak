package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Images {

    private final List<Image> baseImages;

    private final List<Image> hdpImages;

    private final List<Image> hdfImages;

    private final List<Image> cdhImages;

    private final Set<String> suppertedVersions;

    @JsonCreator
    public Images(
            @JsonProperty("base-images") List<Image> baseImages,
            @JsonProperty("hdp-images") List<Image> hdpImages,
            @JsonProperty("hdf-images") List<Image> hdfImages,
            @JsonProperty("cdh-images") List<Image> cdhImages,
            @JsonProperty("supported-cb-versions") Set<String> suppertedVersions) {
        this.baseImages = (baseImages == null) ? emptyList() : baseImages;
        this.hdpImages = (hdpImages == null) ? emptyList() : hdpImages;
        this.hdfImages = (hdfImages == null) ? emptyList() : hdfImages;
        this.cdhImages = (cdhImages == null) ? emptyList() : cdhImages;
        this.suppertedVersions = (suppertedVersions == null) ? emptySet() : suppertedVersions;
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

    public List<Image> getCdhImages() {
        return cdhImages;
    }

    public Set<String> getSuppertedVersions() {
        return suppertedVersions;
    }
}
