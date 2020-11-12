package com.sequenceiq.cloudbreak.service.image;

public class ImageChangeDto {

    private final Long stackId;

    private final String imageId;

    private final String imageCatalogName;

    private final String imageCatalogUrl;

    public ImageChangeDto(Long stackId, String imageId, String imageCatalogName, String imageCatalogUrl) {
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
}
