package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@NotNull
public class ImagesV4Response implements JsonEntity {

    private List<BaseImageV4Response> baseImages;

    private List<ImageV4Response> cdhImages;

    private List<ImageV4Response> freeipaImages;

    private Set<String> supportedVersions;

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
