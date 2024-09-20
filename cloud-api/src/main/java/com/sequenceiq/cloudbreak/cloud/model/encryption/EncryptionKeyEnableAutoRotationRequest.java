package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record EncryptionKeyEnableAutoRotationRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResources,
        Integer rotationPeriodInDays) {

    public EncryptionKeyEnableAutoRotationRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResources,
                builder.rotationPeriodInDays
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private List<CloudResource> cloudResources;

        private Integer rotationPeriodInDays;

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withCloudResources(List<CloudResource> cloudResources) {
            this.cloudResources = cloudResources;
            return this;
        }

        public Builder withRotationPeriodInDays(Integer rotationPeriodInDays) {
            this.rotationPeriodInDays = rotationPeriodInDays;
            return this;
        }

        public EncryptionKeyEnableAutoRotationRequest build() {
            return new EncryptionKeyEnableAutoRotationRequest(this);
        }
    }
}
