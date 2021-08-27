package com.sequenceiq.cloudbreak.cloud.model.encryption;

public class CreateAwsDiskEncryptionParameters {

        private final String encryptionKeyArn;

        private CreateAwsDiskEncryptionParameters(CreateAwsDiskEncryptionParameters.Builder builder) {
            this.encryptionKeyArn = builder.encryptionKeyArn;

        }

        public String getEncryptionKeyArn() {
            return encryptionKeyArn;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("CreateAwsDiskEncryptionParameters{");
            sb.append("encryptionKeyArn='").append(encryptionKeyArn).append('\'');
            sb.append('}');
            return sb.toString();
        }

        public static final class Builder {

            private String encryptionKeyArn;

            public Builder() {
            }

            public CreateAwsDiskEncryptionParameters.Builder withEncryptionKeyArn(String encryptionKeyArn) {
                this.encryptionKeyArn = encryptionKeyArn;
                return this;
            }
        }

    }
