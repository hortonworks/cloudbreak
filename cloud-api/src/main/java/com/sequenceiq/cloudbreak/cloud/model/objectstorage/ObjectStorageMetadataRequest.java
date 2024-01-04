package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.common.model.ObjectStorageType;

public class ObjectStorageMetadataRequest implements CloudPlatformAware {

    private @NotNull CloudCredential credential;

    private @NotNull String cloudPlatform;

    private @NotNull @NotEmpty @ValidObjectStoragePathLength String objectStoragePath;

    private @NotNull String region;

    private ObjectStorageType objectStorageType;

    public ObjectStorageMetadataRequest() {
    }

    public ObjectStorageMetadataRequest(Builder builder) {
        this.credential = builder.credential;
        this.cloudPlatform = builder.cloudPlatform;
        this.objectStoragePath = builder.objectStoragePath;
        this.region = builder.region;
        this.objectStorageType = builder.objectStorageType;
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

    public String getRegion() {
        return region;
    }

    public ObjectStorageType getObjectStorageType() {
        return objectStorageType;
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
                Objects.equals(objectStoragePath, request.objectStoragePath) &&
                Objects.equals(region, request.region);
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
                ", region='" + region + '\'' +
                '}';
    }

    @Override
    public Platform platform() {
        return Platform.platform(cloudPlatform);
    }

    @Override
    public Variant variant() {
        return Variant.variant(cloudPlatform);
    }

    public static class Builder {

        private CloudCredential credential;

        private String cloudPlatform;

        private String objectStoragePath;

        private String region;

        private ObjectStorageType objectStorageType;

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

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withObjectStorageType(ObjectStorageType objectStorageType) {
            this.objectStorageType = objectStorageType;
            return this;
        }

        public ObjectStorageMetadataRequest build() {
            return new ObjectStorageMetadataRequest(this);
        }
    }
}
