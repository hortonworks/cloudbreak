package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FreeIpaUpgradeOptionsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaUpgradeOptions {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ImageInfoResponse> images = new ArrayList<>();

    private ImageInfoResponse currentImage;

    public List<ImageInfoResponse> getImages() {
        return images;
    }

    public void setImages(List<ImageInfoResponse> images) {
        this.images = images;
    }

    public ImageInfoResponse getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(ImageInfoResponse currentImage) {
        this.currentImage = currentImage;
    }

    @Override
    public String toString() {
        return "FreeIpaUpgradeOptions{" +
                "images=" + images +
                ", currentImage=" + currentImage +
                '}';
    }
}
