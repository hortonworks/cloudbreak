package com.sequenceiq.cloudbreak.service.image;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeComponents;

public class ImageChangeDto {

    private final Long stackId;

    private final String imageId;

    private final String imageCatalogName;

    private final String imageCatalogUrl;

    private final UpgradeComponents components;

    public ImageChangeDto(Long stackId, String imageId, String imageCatalogName, String imageCatalogUrl) {
        this(stackId, imageId, imageCatalogName, imageCatalogUrl, null);
    }

    @JsonCreator
    public ImageChangeDto(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("imageCatalogName") String imageCatalogName,
            @JsonProperty("imageCatalogUrl") String imageCatalogUrl,
            @JsonProperty("components") UpgradeComponents components) {
        this.stackId = stackId;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.imageCatalogUrl = imageCatalogUrl;
        this.components = components;
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

    public UpgradeComponents getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ImageChangeDto.class.getSimpleName() + "[", "]")
                .add("stackId=" + stackId)
                .add("components=" + components)
                .add("imageId='" + imageId + "'")
                .add("imageCatalogName='" + imageCatalogName + "'")
                .add("imageCatalogUrl='" + imageCatalogUrl + "'")
                .toString();
    }
}
