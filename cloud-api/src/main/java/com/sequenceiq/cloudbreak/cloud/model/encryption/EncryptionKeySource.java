package com.sequenceiq.cloudbreak.cloud.model.encryption;

public record EncryptionKeySource(EncryptionKeyType keyType, String keyValue) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private EncryptionKeyType keyType;

        private String keyValue;

        private Builder() {
        }

        public Builder withKeyType(EncryptionKeyType keyType) {
            this.keyType = keyType;
            return this;
        }

        public Builder withKeyValue(String keyValue) {
            this.keyValue = keyValue;
            return this;
        }

        public EncryptionKeySource build() {
            return new EncryptionKeySource(keyType, keyValue);
        }
    }
}
