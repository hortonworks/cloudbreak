package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;

public record UpdateCloudSecretRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudResource cloudResource,
        Optional<String> newSecretValue, Optional<EncryptionKeySource> newEncryptionKeySource) {

    public UpdateCloudSecretRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResource,
                builder.newSecretValue,
                builder.newEncryptionKeySource
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "UpdateCloudSecretRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", cloudResource=" + cloudResource +
                ", newEncryptionKeySource=" + newEncryptionKeySource +
                '}';
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private CloudResource cloudResource;

        private Optional<String> newSecretValue;

        private Optional<EncryptionKeySource> newEncryptionKeySource;

        private Builder() {
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withCloudResource(CloudResource cloudResource) {
            this.cloudResource = cloudResource;
            return this;
        }

        public Builder withNewSecretValue(Optional<String> newSecretValue) {
            this.newSecretValue = newSecretValue;
            return this;
        }

        public Builder withNewEncryptionKeySource(Optional<EncryptionKeySource> newEncryptionKeySource) {
            this.newEncryptionKeySource = newEncryptionKeySource;
            return this;
        }

        public UpdateCloudSecretRequest build() {
            return new UpdateCloudSecretRequest(this);
        }
    }
}
