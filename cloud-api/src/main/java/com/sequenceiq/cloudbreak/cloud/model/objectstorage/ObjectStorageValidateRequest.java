package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;

public class ObjectStorageValidateRequest implements CloudPlatformAware {

    private @NotNull CloudCredential credential;

    private @NotNull String cloudPlatform;

    private @NotNull CloudStorageRequest cloudStorageRequest;

    private SpiFileSystem spiFileSystem;

    public ObjectStorageValidateRequest() {
    }

    public ObjectStorageValidateRequest(Builder builder) {
        this.credential = builder.credential;
        this.cloudPlatform = builder.cloudPlatform;
        this.cloudStorageRequest = builder.cloudStorageRequest;
        this.spiFileSystem = builder.spiFileSystem;
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

    public CloudStorageRequest getCloudStorageRequest() {
        return cloudStorageRequest;
    }

    public void setCloudStorageRequest(CloudStorageRequest cloudStorageRequest) {
        this.cloudStorageRequest = cloudStorageRequest;
    }

    public SpiFileSystem getSpiFileSystem() {
        return spiFileSystem;
    }

    public void setSpiFileSystem(SpiFileSystem spiFileSystem) {
        this.spiFileSystem = spiFileSystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ObjectStorageValidateRequest request = (ObjectStorageValidateRequest) o;
        return Objects.equals(credential, request.credential) &&
                Objects.equals(cloudPlatform, request.cloudPlatform) &&
                Objects.equals(cloudStorageRequest, request.cloudStorageRequest) &&
                Objects.equals(spiFileSystem, request.spiFileSystem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credential, cloudPlatform, cloudStorageRequest, spiFileSystem);
    }

    @Override
    public String toString() {
        return "ObjectStorageMetadataRequest{" +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", cloudStorageRequest='" + JsonUtil.writeValueAsStringSilent(cloudStorageRequest) + '\'' +
                ", spiFileSystem='" + spiFileSystem + '\'' +
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

        private CloudStorageRequest cloudStorageRequest;

        private SpiFileSystem spiFileSystem;

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withCloudStorageRequest(CloudStorageRequest cloudStorageRequest) {
            this.cloudStorageRequest = cloudStorageRequest;
            return this;
        }

        public Builder withSpiFileSystem(SpiFileSystem spiFileSystem) {
            this.spiFileSystem = spiFileSystem;
            return this;
        }

        public ObjectStorageValidateRequest build() {
            return new ObjectStorageValidateRequest(this);
        }
    }
}
