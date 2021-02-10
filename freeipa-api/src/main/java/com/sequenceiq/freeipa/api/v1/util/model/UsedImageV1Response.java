package com.sequenceiq.freeipa.api.v1.util.model;

import java.util.Objects;

public class UsedImageV1Response {

    private String imageId;

    public UsedImageV1Response(String imageId) {
        this.imageId = imageId;
    }

    public UsedImageV1Response() {
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UsedImageV1Response)) {
            return false;
        }
        UsedImageV1Response that = (UsedImageV1Response) o;
        return Objects.equals(imageId, that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId);
    }

    @Override
    public String toString() {
        return "UsedImageV1Response{" +
                "imageId='" + imageId + '\'' +
                '}';
    }
}
