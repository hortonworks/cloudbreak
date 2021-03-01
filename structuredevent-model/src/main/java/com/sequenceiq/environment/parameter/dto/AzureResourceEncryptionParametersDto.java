package com.sequenceiq.environment.parameter.dto;

public class AzureResourceEncryptionParametersDto {
    private final String keyUrl;

    public AzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.Builder builder) {
        keyUrl = builder.keyUrl;
    }

    public String getKeyUrl() {
        return keyUrl;
    }

    public static AzureResourceEncryptionParametersDto.Builder builder() {
        return new AzureResourceEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParametersDto{" +
                "keyUrl=" + keyUrl +
                '}';
    }

    public static final class Builder {
        private String keyUrl;

        public AzureResourceEncryptionParametersDto.Builder withKeyUrl(String keyUrl) {
            this.keyUrl = keyUrl;
            return this;
        }

        public AzureResourceEncryptionParametersDto build() {
            return new AzureResourceEncryptionParametersDto(this);
        }
    }
}
