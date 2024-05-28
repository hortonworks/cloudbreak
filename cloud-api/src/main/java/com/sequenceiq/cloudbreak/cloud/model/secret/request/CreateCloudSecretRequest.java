package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;

public record CreateCloudSecretRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResources, String secretName,
        String description, String secretValue, Optional<EncryptionKeySource> encryptionKeySource, Map<String, String> tags) {

    public CreateCloudSecretRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResources,
                builder.secretName,
                builder.description,
                builder.secretValue,
                builder.encryptionKeySource,
                builder.tags
        );
    }

    public Builder toBuilder() {
        return builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResources(cloudResources)
                .withSecretName(secretName)
                .withDescription(description)
                .withSecretValue(secretValue)
                .withEncryptionKeySource(encryptionKeySource)
                .withTags(tags);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "CreateCloudSecretRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                ", cloudResources=" + cloudResources +
                ", secretName='" + secretName + '\'' +
                ", description='" + description + '\'' +
                ", encryptionKeySource=" + encryptionKeySource +
                ", tags=" + tags +
                '}';
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private List<CloudResource> cloudResources = Collections.emptyList();

        private String secretName;

        private String description;

        private String secretValue;

        private Optional<EncryptionKeySource> encryptionKeySource = Optional.empty();

        private Map<String, String> tags = Collections.emptyMap();

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

        public Builder withCloudResources(List<CloudResource> cloudResource) {
            this.cloudResources = cloudResource;
            return this;
        }

        public Builder withSecretName(String secretName) {
            this.secretName = secretName;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withSecretValue(String secretValue) {
            this.secretValue = secretValue;
            return this;
        }

        public Builder withEncryptionKeySource(Optional<EncryptionKeySource> encryptionKeySource) {
            this.encryptionKeySource = encryptionKeySource;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public CreateCloudSecretRequest build() {
            return new CreateCloudSecretRequest(this);
        }
    }
}
