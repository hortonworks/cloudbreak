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

    private final List<Image> cdhImages;

    private final List<Image> freeIpaImages;

    private final Set<String> suppertedVersions;

    @JsonCreator
    public Images(
            @JsonProperty("base-images") List<Image> baseImages,
            @JsonProperty("cdh-images") List<Image> cdhImages,
            @JsonProperty("freeipa-images") List<Image> freeIpaImages,
            @JsonProperty("supported-cb-versions") Set<String> suppertedVersions) {
        this.baseImages = (baseImages == null) ? emptyList() : baseImages;
        this.cdhImages = (cdhImages == null) ? emptyList() : cdhImages;
        this.freeIpaImages = (freeIpaImages == null) ? emptyList() : freeIpaImages;
        this.suppertedVersions = (suppertedVersions == null) ? emptySet() : suppertedVersions;
    }

    public List<Image> getBaseImages() {
        return baseImages;
    }

    public List<Image> getCdhImages() {
        return cdhImages;
    }

    public List<Image> getFreeIpaImages() {
        return freeIpaImages;
    }

    public Set<String> getSuppertedVersions() {
        return suppertedVersions;
    }

    public int getNumberOfImages() {
        return baseImages.size() + cdhImages.size() + freeIpaImages.size();
    }
}
