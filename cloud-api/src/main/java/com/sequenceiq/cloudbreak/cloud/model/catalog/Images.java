package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Images {

    private static final String BASE_IMAGES = "base-images";

    private static final String CDH_IMAGES = "cdh-images";

    private static final String FREEIPA_IMAGES = "freeipa-images";

    private final List<Image> baseImages;

    private final List<Image> cdhImages;

    private final List<Image> freeIpaImages;

    private final Set<String> suppertedVersions;

    @JsonCreator
    public Images(
            @JsonProperty(BASE_IMAGES) List<Image> baseImages,
            @JsonProperty(CDH_IMAGES) List<Image> cdhImages,
            @JsonProperty(FREEIPA_IMAGES) List<Image> freeIpaImages,
            @JsonProperty("supported-cb-versions") Set<String> suppertedVersions) {
        this.baseImages = (baseImages == null) ? emptyList() : baseImages;
        this.cdhImages = (cdhImages == null) ? emptyList() : cdhImages;
        this.freeIpaImages = (freeIpaImages == null) ? emptyList() : freeIpaImages;
        this.suppertedVersions = (suppertedVersions == null) ? emptySet() : suppertedVersions;
    }

    @JsonProperty(BASE_IMAGES)
    public List<Image> getBaseImages() {
        return baseImages;
    }

    @JsonProperty(CDH_IMAGES)
    public List<Image> getCdhImages() {
        return cdhImages;
    }

    @JsonProperty(FREEIPA_IMAGES)
    public List<Image> getFreeIpaImages() {
        return freeIpaImages;
    }

    @JsonIgnore
    public Set<String> getSuppertedVersions() {
        return suppertedVersions;
    }

    @JsonIgnore
    public int getNumberOfImages() {
        return baseImages.size() + cdhImages.size() + freeIpaImages.size();
    }
}
