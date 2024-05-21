package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public record EncryptRequest(String input, EncryptionKeySource keySource, CloudCredential cloudCredential, String regionName,
        Map<String, String> encryptionContext) {

    EncryptRequest(Builder builder) {
        this(builder.input, builder.keySource, builder.cloudCredential, builder.regionName, builder.encryptionContext);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private static final String ENVIRONMENT_CRN_KEY = "ENVIRONMENT_CRN";

        private static final String SECRET_NAME_KEY = "SECRET_NAME";

        private String input;

        private EncryptionKeySource keySource;

        private CloudCredential cloudCredential;

        private String regionName;

        private Map<String, String> encryptionContext = new HashMap<>();

        private Builder() {
        }

        public Builder withInput(String input) {
            this.input = input;
            return this;
        }

        public Builder withKeySource(EncryptionKeySource keySource) {
            this.keySource = keySource;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withRegionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        public Builder withEncryptionContext(Map<String, String> encryptionContext) {
            this.encryptionContext = encryptionContext;
            return this;
        }

        public Builder withEncryptionContextEntry(String key, String value) {
            encryptionContext.put(key, value);
            return this;
        }

        public Builder withEnvironmentCrn(String environmentCrn) {
            encryptionContext.put(ENVIRONMENT_CRN_KEY, environmentCrn);
            return this;
        }

        public Builder withSecretName(String secretName) {
            encryptionContext.put(SECRET_NAME_KEY, secretName);
            return this;
        }

        public EncryptRequest build() {
            return new EncryptRequest(this);
        }
    }
}
