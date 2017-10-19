package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel("ImagesResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImagesResponse implements JsonEntity {

    private List<BaseImageResponse> baseImages;

    private List<ImageResponse> hdpImages;

    private List<ImageResponse> hdfImages;

    public List<BaseImageResponse> getBaseImages() {
        return baseImages;
    }

    public void setBaseImages(List<BaseImageResponse> baseImages) {
        this.baseImages = baseImages;
    }

    public List<ImageResponse> getHdpImages() {
        return hdpImages;
    }

    public void setHdpImages(List<ImageResponse> hdpImages) {
        this.hdpImages = hdpImages;
    }

    public List<ImageResponse> getHdfImages() {
        return hdfImages;
    }

    public void setHdfImages(List<ImageResponse> hdfImages) {
        this.hdfImages = hdfImages;
    }
}
