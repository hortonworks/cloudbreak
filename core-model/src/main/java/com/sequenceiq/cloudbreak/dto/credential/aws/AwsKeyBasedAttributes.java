package com.sequenceiq.cloudbreak.dto.credential.aws;

import java.io.Serializable;

public class AwsKeyBasedAttributes implements Serializable {

    private final String accessKey;

    private final String secretKey;

    private AwsKeyBasedAttributes(Builder builder) {
        accessKey = builder.accessKey;
        secretKey = builder.secretKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public static final class Builder {
        private String accessKey;

        private String secretKey;

        public Builder accessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public AwsKeyBasedAttributes build() {
            return new AwsKeyBasedAttributes(this);
        }
    }
}
