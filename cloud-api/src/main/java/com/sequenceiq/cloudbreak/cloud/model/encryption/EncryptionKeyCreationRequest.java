package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record EncryptionKeyCreationRequest(
        String keyName, CloudContext cloudContext, CloudCredential cloudCredential, Map<String, String> tags,
        String description, List<CloudResource> cloudResources,
        List<String> cryptographicPrincipals) {

    private EncryptionKeyCreationRequest(Builder builder) {
        this(
                builder.keyName,
                builder.cloudContext,
                builder.cloudCredential,
                builder.tags,
                builder.description,
                builder.cloudResources,
                builder.cryptographicPrincipals
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String keyName;

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private Map<String, String> tags = new HashMap<>();

        private String description;

        private List<CloudResource> cloudResources = new ArrayList<>();

        private List<String> cryptographicPrincipals = new ArrayList<>();

        private Builder() {
            // Prohibit instantiation outside the enclosing class
        }

        public Builder withKeyName(String keyName) {
            this.keyName = keyName;
            return this;
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withCloudResources(List<CloudResource> cloudResources) {
            this.cloudResources = cloudResources;
            return this;
        }

        public Builder withCryptographicPrincipals(List<String> cryptographicPrincipals) {
            this.cryptographicPrincipals = cryptographicPrincipals;
            return this;
        }

        public EncryptionKeyCreationRequest build() {
            return new EncryptionKeyCreationRequest(this);
        }

    }

}