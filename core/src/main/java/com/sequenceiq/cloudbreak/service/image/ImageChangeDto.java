package com.sequenceiq.cloudbreak.service.image;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageChangeDto {

    private final Long stackId;

    private final String imageId;

    private final String imageCatalogName;

    private final String imageCatalogUrl;

    @JsonCreator
    public ImageChangeDto(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl) {
        this.stackId = stackId;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public ImageChangeDto(Long stackId, String imageId) {
        this(stackId, imageId, null, null);
    }

    public Long getStackId() {
        return stackId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ImageChangeDto.class.getSimpleName() + "[", "]")
                .add("stackId=" + stackId)
                .add("imageId='" + imageId + "'")
                .add("imageCatalogName='" + imageCatalogName + "'")
                .add("imageCatalogUrl='" + imageCatalogUrl + "'")
                .toString();
    }
}
