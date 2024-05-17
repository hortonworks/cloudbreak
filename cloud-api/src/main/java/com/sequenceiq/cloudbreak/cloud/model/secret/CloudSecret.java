package com.sequenceiq.cloudbreak.cloud.model.secret;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;

public record CloudSecret(String secretId, String secretName, String description, String secretValue, EncryptionKeySource keySource, Instant deletionDate,
        Map<String, String> tags, List<String> cryptographicPrincipals, List<String> cryptographicAuthorizedClients) {

    public CloudSecret(Builder builder) {
        this(
                Preconditions.checkNotNull(builder.secretId),
                Preconditions.checkNotNull(builder.secretName),
                builder.description,
                builder.secretValue,
                builder.keySource,
                builder.deletionDate,
                builder.tags,
                builder.cryptographicPrincipals,
                builder.cryptographicAuthorizedClients
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
                .withCryptographicPrincipals(cryptographicPrincipals)
                .withCryptographicAuthorizedClients(cryptographicAuthorizedClients);
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
                ", cryptographicPrincipals=" + cryptographicPrincipals +
                ", cryptographicAuthorizedClients=" + cryptographicAuthorizedClients +
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

        private List<String> cryptographicPrincipals;

        private List<String> cryptographicAuthorizedClients;

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

        public Builder withCryptographicPrincipals(List<String> cryptographicPrincipals) {
            this.cryptographicPrincipals = cryptographicPrincipals;
            return this;
        }

        public Builder withCryptographicAuthorizedClients(List<String> cryptographicAuthorizedClients) {
            this.cryptographicAuthorizedClients = cryptographicAuthorizedClients;
            return this;
        }

        public CloudSecret build() {
            return new CloudSecret(this);
        }

    }

}
