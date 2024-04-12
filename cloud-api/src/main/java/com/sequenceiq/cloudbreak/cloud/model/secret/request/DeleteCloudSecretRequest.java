package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record DeleteCloudSecretRequest(CloudContext cloudContext, CloudCredential cloudCredential, List<CloudResource> cloudResources, String secretName) {

    public DeleteCloudSecretRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResources,
                builder.secretName
        );
    }

    public Builder toBuilder() {
        return builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResources(cloudResources)
                .withSecretName(secretName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private List<CloudResource> cloudResources = Collections.emptyList();

        private String secretName;

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

        public Builder withCloudResources(List<CloudResource> cloudResources) {
            this.cloudResources = cloudResources;
            return this;
        }

        public Builder withSecretName(String secretName) {
            this.secretName = secretName;
            return this;
        }

        public DeleteCloudSecretRequest build() {
            return new DeleteCloudSecretRequest(this);
        }
    }
}
