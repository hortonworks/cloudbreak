package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

public class ObjectStorageMetadataRequest {

    private String objectStoragePath;

    public ObjectStorageMetadataRequest() {
    }

    public ObjectStorageMetadataRequest(Builder builder) {
        this.objectStoragePath = builder.objectStoragePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getObjectStoragePath() {
        return objectStoragePath;
    }

    public void setObjectStoragePath(String objectStoragePath) {
        this.objectStoragePath = objectStoragePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectStorageMetadataRequest that = (ObjectStorageMetadataRequest) o;
        return Objects.equals(objectStoragePath, that.objectStoragePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectStoragePath);
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataRequest{" +
                "objectStoragePath='" + objectStoragePath + '\'' +
                '}';
    }

    public static class Builder {

        private String objectStoragePath;

        public Builder withObjectStoragePath(String objectStoragePath) {
            this.objectStoragePath = objectStoragePath;
            return this;
        }

        public ObjectStorageMetadataRequest build() {
            return new ObjectStorageMetadataRequest(this);
        }
    }
}
