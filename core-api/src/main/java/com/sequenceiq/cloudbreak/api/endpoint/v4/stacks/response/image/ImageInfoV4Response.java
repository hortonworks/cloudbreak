package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import java.util.Objects;

public class ImageInfoV4Response implements Comparable<ImageInfoV4Response> {

    private String imageName;

    private String imageId;

    private String imageCatalogName;

    private long created;

    private ImageComponentVersions componentVersions;

    public ImageInfoV4Response() {
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.created = created;
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created, ImageComponentVersions componentVersions) {
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

    public ImageComponentVersions getComponentVersions() {
        return componentVersions;
    }

    public void setComponentVersions(ImageComponentVersions componentVersions) {
        this.componentVersions = componentVersions;
    }

    public ImageInfoV4Response imageId(String imageId) {
        this.imageId = imageId;
        return this;
    }

    public ImageInfoV4Response imageCatalogName(String catalogName) {
        this.imageCatalogName = catalogName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImageInfoV4Response that = (ImageInfoV4Response) o;
        return created == that.created &&
                Objects.equals(imageName, that.imageName) &&
                Objects.equals(imageId, that.imageId) &&
                Objects.equals(imageCatalogName, that.imageCatalogName) &&
                Objects.equals(componentVersions, that.componentVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, imageId, imageCatalogName, created, componentVersions);
    }

    @Override
    public int compareTo(ImageInfoV4Response o) {
        int ret = 0;
        if (componentVersions != null && o != null && o.componentVersions != null) {
            ret = componentVersions.compareTo(o.componentVersions);
        }
        return ret;
    }
}
