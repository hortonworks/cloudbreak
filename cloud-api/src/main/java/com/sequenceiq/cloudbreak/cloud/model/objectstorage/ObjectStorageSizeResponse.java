package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

public class ObjectStorageSizeResponse {

    private double storageInBytes;

    public ObjectStorageSizeResponse() {
    }

    public ObjectStorageSizeResponse(Builder builder) {
        this.storageInBytes = builder.storageInBytes;
    }

    public double getStorageInBytes() {
        return storageInBytes;
    }

    public void setStorageInBytes(double storageInBytes) {
        this.storageInBytes = storageInBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ObjectStorageSizeResponse{" +
                "storageInBytes=" + storageInBytes +
                '}';
    }

    public static class Builder {

        private double storageInBytes;

        public Builder withStorageInBytes(double storageInBytes) {
            this.storageInBytes = storageInBytes;
            return this;
        }

        public ObjectStorageSizeResponse build() {
            return new ObjectStorageSizeResponse(this);
        }
    }
}
