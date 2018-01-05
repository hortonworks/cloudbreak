package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;

public class StatedImages {

    private Images images;

    private String imageCatalogUrl;

    private StatedImages(Images images, String imageCatalogUrl) {
        this.images = images;
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public Images getImages() {
        return images;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public static StatedImages statedImages(Images images, String imageCatalogUrl) {
        return new StatedImages(images, imageCatalogUrl);
    }
}
