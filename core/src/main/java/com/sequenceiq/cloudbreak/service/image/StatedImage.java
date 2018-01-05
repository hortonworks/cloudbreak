package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class StatedImage {

    private  com.sequenceiq.cloudbreak.cloud.model.catalog.Image image;

    private String imageCatalogUrl;

    private StatedImage(Image image, String imageCatalogUrl) {
        this.image = image;
        this.imageCatalogUrl = imageCatalogUrl;
    }

    public Image getImage() {
        return image;
    }

    public String getImageCatalogUrl() {
        return imageCatalogUrl;
    }

    public static StatedImage statedImage(Image image, String imageCatalogUrl) {
        return new StatedImage(image, imageCatalogUrl);
    }
}
