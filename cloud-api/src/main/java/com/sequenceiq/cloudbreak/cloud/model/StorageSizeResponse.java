package com.sequenceiq.cloudbreak.cloud.model;

public class StorageSizeResponse {

    private double storageInBytes;

    public StorageSizeResponse() {
    }

    public StorageSizeResponse(Builder builder) {
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

        public StorageSizeResponse build() {
            return new StorageSizeResponse(this);
        }
    }
}
