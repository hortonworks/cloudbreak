package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class ObjectStorageMetadataRequest {

    private @NotNull CloudCredential credential;

    private @NotNull String cloudPlatform;

    private @NotNull String objectStoragePath;

    public ObjectStorageMetadataRequest() {
    }

    public ObjectStorageMetadataRequest(Builder builder) {
        this.credential = builder.credential;
        this.cloudPlatform = builder.cloudPlatform;
        this.objectStoragePath = builder.objectStoragePath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public CloudCredential getCredential() {
        return credential;
    }

    public void setCredential(CloudCredential credential) {
        this.credential = credential;
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
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ObjectStorageMetadataRequest request = (ObjectStorageMetadataRequest) o;
        return Objects.equals(credential, request.credential) &&
                Objects.equals(cloudPlatform, request.cloudPlatform) &&
                Objects.equals(objectStoragePath, request.objectStoragePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credential, cloudPlatform, objectStoragePath);
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataRequest{" +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", objectStoragePath='" + objectStoragePath + '\'' +
                '}';
    }

    public static class Builder {

        private CloudCredential credential;

        private String cloudPlatform;

        private String objectStoragePath;

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withObjectStoragePath(String objectStoragePath) {
            this.objectStoragePath = objectStoragePath;
            return this;
        }

        public ObjectStorageMetadataRequest build() {
            return new ObjectStorageMetadataRequest(this);
        }
    }
}
