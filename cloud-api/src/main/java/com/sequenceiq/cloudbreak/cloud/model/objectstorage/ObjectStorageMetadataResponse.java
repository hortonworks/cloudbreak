package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

public class ObjectStorageMetadataResponse {

    private String region;

    private ResponseStatus status;

    public ObjectStorageMetadataResponse() {
    }

    public ObjectStorageMetadataResponse(Builder builder) {
        this.region = builder.region;
        this.status = builder.status;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataResponse{" +
                "region='" + region + '\'' +
                ", status=" + status +
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
        ObjectStorageMetadataResponse response = (ObjectStorageMetadataResponse) o;
        return Objects.equals(region, response.region) &&
                status == response.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, status);
    }

    public static class Builder {

        private String region;

        private ResponseStatus status;

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withStatus(ResponseStatus status) {
            this.status = status;
            return this;
        }

        public ObjectStorageMetadataResponse build() {
            return new ObjectStorageMetadataResponse(this);
        }
    }

    public enum ResponseStatus {
        OK,
        ACCESS_DENIED
    }
}
