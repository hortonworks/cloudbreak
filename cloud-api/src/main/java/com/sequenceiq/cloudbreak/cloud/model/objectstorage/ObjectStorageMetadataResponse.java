package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

public class ObjectStorageMetadataResponse {

    private String region;

    public ObjectStorageMetadataResponse() {
    }

    public ObjectStorageMetadataResponse(Builder builder) {
        this.region = builder.region;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataResponse{" +
                "region='" + region + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ObjectStorageMetadataResponse that = (ObjectStorageMetadataResponse) o;
        return Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region);
    }

    public static class Builder {

        private String region;

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public ObjectStorageMetadataResponse build() {
            return new ObjectStorageMetadataResponse(this);
        }
    }
}
