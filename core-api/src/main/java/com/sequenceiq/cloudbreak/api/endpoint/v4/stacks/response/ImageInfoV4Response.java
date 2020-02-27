package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.Map;

public class ImageInfoV4Response {

    private String imageName;

    private String imageId;

    private String imageCatalogName;

    private long created;

    private Map<String, String> componentVersions;

    public ImageInfoV4Response() {
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.created = created;
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created, Map<String, String> componentVersions) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.created = created;
        this.componentVersions = componentVersions;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
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

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public Map<String, String> getComponentVersions() {
        return componentVersions;
    }

    public void setComponentVersions(Map<String, String> componentVersions) {
        this.componentVersions = componentVersions;
    }
}
