package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaUpgradeOptionsV1")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaUpgradeOptions {

    private List<ImageInfoResponse> images;

    public List<ImageInfoResponse> getImages() {
        return images;
    }

    public void setImages(List<ImageInfoResponse> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "FreeIpaUpgradeOptions{" +
                "images=" + images +
                '}';
    }
}
