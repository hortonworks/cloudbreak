package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AwsDiskEncryptionParametersDto.Builder.class)
public class AwsDiskEncryptionParametersDto {

    private final String encryptionKeyArn;

    private AwsDiskEncryptionParametersDto(AwsDiskEncryptionParametersDto.Builder builder) {
        encryptionKeyArn = builder.encryptionKeyArn;
    }

    public String getEncryptionKeyArn() {
        return encryptionKeyArn;
    }

    public static AwsDiskEncryptionParametersDto.Builder builder() {
        return new AwsDiskEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "AwsDiskEncryptionParametersDto{" +
                "encryptionKeyArn=" + encryptionKeyArn +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String encryptionKeyArn;

        public AwsDiskEncryptionParametersDto.Builder withEncryptionKeyArn(String encryptionKeyArn) {
            this.encryptionKeyArn = encryptionKeyArn;
            return this;
        }

        public AwsDiskEncryptionParametersDto build() {
            return new AwsDiskEncryptionParametersDto(this);
        }
    }
}
