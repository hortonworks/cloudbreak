package com.sequenceiq.cloudbreak.cloud.model.secret;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;

public record CloudSecret(String secretId, String secretName, String description, String secretValue, EncryptionKeySource keySource, Instant deletionDate,
        Map<String, String> tags, List<String> principals, List<String> authorizedClients) {

    public CloudSecret(Builder builder) {
        this(
                Preconditions.checkNotNull(builder.secretId),
                Preconditions.checkNotNull(builder.secretName),
                builder.description,
                builder.secretValue,
                builder.keySource,
                builder.deletionDate,
                builder.tags,
                builder.principals,
                builder.authorizedClients
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder()
                .withSecretId(secretId)
                .withSecretName(secretName)
                .withDescription(description)
                .withSecretValue(secretValue)
                .withKeySource(keySource)
                .withDeletionDate(deletionDate)
                .withTags(tags)
                .withPrincipals(principals)
                .withAuthorizedClients(authorizedClients);
    }

    @Override
    public String toString() {
        return "CloudSecret{" +
                "secretId='" + secretId + '\'' +
                ", secretName='" + secretName + '\'' +
                ", description='" + description + '\'' +
                ", keySource='" + keySource + '\'' +
                ", deletionDate=" + deletionDate +
                ", tags=" + tags +
                ", principals=" + principals +
                ", authorizedClients=" + authorizedClients +
                '}';
    }

    public static final class Builder {

        private String secretId;

        private String secretName;

        private String description;

        private String secretValue;

        private EncryptionKeySource keySource;

        private Instant deletionDate;

        private Map<String, String> tags;

        private List<String> principals;

        private List<String> authorizedClients;

        private Builder() {
        }

        public Builder withSecretId(String secretId) {
            this.secretId = secretId;
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

        public Builder withKeySource(EncryptionKeySource keySource) {
            this.keySource = keySource;
            return this;
        }

        public Builder withDeletionDate(Instant deletionDate) {
            this.deletionDate = deletionDate;
            return this;
        }

        public Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withPrincipals(List<String> principals) {
            this.principals = principals;
            return this;
        }

        public Builder withAuthorizedClients(List<String> authorizedClients) {
            this.authorizedClients = authorizedClients;
            return this;
        }

        public CloudSecret build() {
            return new CloudSecret(this);
        }
    }
}
