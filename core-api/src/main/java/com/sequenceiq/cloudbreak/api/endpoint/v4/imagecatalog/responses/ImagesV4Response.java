package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ImagesV4Response implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<BaseImageV4Response> baseImages = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImageV4Response> cdhImages = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImageV4Response> freeipaImages = new ArrayList<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> supportedVersions = new HashSet<>();

    public List<BaseImageV4Response> getBaseImages() {
        return baseImages;
    }

    public void setBaseImages(List<BaseImageV4Response> baseImages) {
        this.baseImages = baseImages;
    }

    public List<ImageV4Response> getCdhImages() {
        return cdhImages;
    }

    public void setCdhImages(List<ImageV4Response> cdhImages) {
        this.cdhImages = cdhImages;
    }

    public void setSupportedVersions(Set<String> supportedVersions) {
        this.supportedVersions = supportedVersions;
    }

    public Set<String> getSupportedVersions() {
        return supportedVersions;
    }

    public List<ImageV4Response> getFreeipaImages() {
        return freeipaImages;
    }

    public void setFreeipaImages(List<ImageV4Response> freeipaImages) {
        this.freeipaImages = freeipaImages;
    }

    @Override
    public String toString() {
        return "ImagesV4Response{" +
                "baseImages=" + baseImages +
                ", cdhImages=" + cdhImages +
                ", freeipaImages=" + freeipaImages +
                ", supportedVersions=" + supportedVersions +
                '}';
    }
}
