package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class DiskEncryptionSetDeletionRequest implements CloudPlatformAware {

    private final CloudCredential cloudCredential;

    private final CloudContext cloudContext;

    private final List<CloudResource> cloudResources;

    private DiskEncryptionSetDeletionRequest(Builder builder) {
        this.cloudCredential = builder.cloudCredential;
        this.cloudContext = builder.cloudContext;
        this.cloudResources = builder.cloudResources;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public List<CloudResource> getCloudResources() {
        return cloudResources;
    }

    @Override
    public Platform platform() {
        return cloudContext.getPlatform();
    }

    @Override
    public Variant variant() {
        return cloudContext.getVariant();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DiskEncryptionSetDeletionRequest{");
        sb.append("cloudCredential=").append(cloudCredential);
        sb.append(", cloudContext=").append(cloudContext);
        sb.append(", cloudResources=").append(cloudResources);
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private CloudCredential cloudCredential;

        private CloudContext cloudContext;

        private List<CloudResource> cloudResources;

        public Builder() {
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudResources(List<CloudResource> cloudResources) {
            this.cloudResources = cloudResources;
            return this;
        }

        public DiskEncryptionSetDeletionRequest build() {
            return new DiskEncryptionSetDeletionRequest(this);
        }

    }

}