package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

public class UpgradeOptionV4Response {

    private String imageId;

    private String imageCatalogName;

    public UpgradeOptionV4Response() {
    }

    public UpgradeOptionV4Response(String imageId, String imageCatalogName) {
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public void setImageCatalogName(String imageCatalogName) {
        this.imageCatalogName = imageCatalogName;
    }
}
