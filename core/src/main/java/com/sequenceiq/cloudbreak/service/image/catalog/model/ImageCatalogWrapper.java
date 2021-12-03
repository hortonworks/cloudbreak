package com.sequenceiq.cloudbreak.service.image.catalog.model;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;

public class ImageCatalogWrapper {

    private CloudbreakImageCatalogV3 imageCatalog;

    private ImageCatalogMetaData imageCatalogMetaData;

    public ImageCatalogWrapper(CloudbreakImageCatalogV3 imageCatalog, ImageCatalogMetaData imageCatalogMetaData) {
        this.imageCatalog = imageCatalog;
        this.imageCatalogMetaData = imageCatalogMetaData;
    }

    public CloudbreakImageCatalogV3 getImageCatalog() {
        return imageCatalog;
    }

    public ImageCatalogMetaData getImageCatalogMetaData() {
        return imageCatalogMetaData;
    }

    @Override
    public String toString() {
        return "ImageCatalogWrapper{" +
                "imageCatalog=" + imageCatalog +
                ", imageCatalogMetaData=" + imageCatalogMetaData +
                '}';
    }
}
