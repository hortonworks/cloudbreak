package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;

public class StatedImages {

    private final Images images;

    private final String imageCatalogUrl;

    private final String imageCatalogName;

    private StatedImages(Images images, String imageCatalogUrl, String imageCatalogName) {
        this.images = images;
        this.imageCatalogUrl = imageCatalogUrl;
        this.imageCatalogName = imageCatalogName;
    }

    public Images getImages() {
        return images;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public static StatedImages statedImages(Images images, String imageCatalogUrl, String imageCatalogName) {
        return new StatedImages(images, imageCatalogUrl, imageCatalogName);
    }
}
