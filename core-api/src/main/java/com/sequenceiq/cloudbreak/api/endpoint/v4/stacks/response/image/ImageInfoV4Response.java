package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image;

import java.util.Objects;

public class ImageInfoV4Response implements Comparable<ImageInfoV4Response> {

    private String imageName;

    private String imageId;

    private String imageCatalogName;

    private long created;

    private String date;

    private ImageComponentVersions componentVersions;

    public ImageInfoV4Response() {
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created, String date) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.created = created;
        this.date = date;
    }

    public ImageInfoV4Response(String imageName, String imageId, String imageCatalogName, long created, String date,
            ImageComponentVersions componentVersions) {
        this.imageName = imageName;
        this.imageId = imageId;
        this.imageCatalogName = imageCatalogName;
        this.created = created;
        this.componentVersions = componentVersions;
        this.date = date;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
                Objects.equals(date, that.date) &&
                Objects.equals(componentVersions, that.componentVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, imageId, imageCatalogName, created, date, componentVersions);
    }

    @Override
    public int compareTo(ImageInfoV4Response o) {
        int ret = 0;
        if (componentVersions != null && o != null && o.componentVersions != null) {
            ret = componentVersions.compareTo(o.componentVersions);
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ImageInfoV4Response{" +
                "imageName='" + imageName + '\'' +
                ", imageId='" + imageId + '\'' +
                '}';
    }
}
