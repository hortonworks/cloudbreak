package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public class DiskEncryptionSetDeletionRequest {

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