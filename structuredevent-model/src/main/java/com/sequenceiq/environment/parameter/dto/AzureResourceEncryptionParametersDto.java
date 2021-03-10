package com.sequenceiq.environment.parameter.dto;

public class AzureResourceEncryptionParametersDto {
    private final String encryptionKeyUrl;

    private AzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.Builder builder) {
        encryptionKeyUrl = builder.encryptionKeyUrl;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public static AzureResourceEncryptionParametersDto.Builder builder() {
        return new AzureResourceEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParametersDto{" +
                "encryptionKeyUrl=" + encryptionKeyUrl +
                '}';
    }

    public static final class Builder {
        private String encryptionKeyUrl;

        public AzureResourceEncryptionParametersDto.Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public AzureResourceEncryptionParametersDto build() {
            return new AzureResourceEncryptionParametersDto(this);
        }
    }
}