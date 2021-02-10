package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

public class UsedImageV4Response {

    private String imageId;

    private String stackVersion;

    public UsedImageV4Response(Image image) {
        this.imageId = image.getImageId();
        this.stackVersion = image.getPackageVersions().get(ImagePackageVersion.STACK.getKey());
    }

    public UsedImageV4Response() {
    }

    public String getImageId() {
        return imageId;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsedImageV4Response)) {
            return false;
        }
        UsedImageV4Response that = (UsedImageV4Response) o;
        return Objects.equals(imageId, that.imageId) && Objects.equals(stackVersion, that.stackVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId, stackVersion);
    }

    @Override
    public String toString() {
        return "UsedImageV4Response{" +
                "imageId='" + imageId + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                '}';
    }
}
