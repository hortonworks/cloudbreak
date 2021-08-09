package com.sequenceiq.environment.parameter.dto;

public class GcpResourceEncryptionParametersDto {

    private final String encryptionKey;

    private GcpResourceEncryptionParametersDto(GcpResourceEncryptionParametersDto.Builder builder) {
        encryptionKey = builder.encryptionKey;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public static GcpResourceEncryptionParametersDto.Builder builder() {
        return new GcpResourceEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "GcpResourceEncryptionParametersDto{" +
                "encryptionKey=" + encryptionKey +
                '}';
    }

    public static final class Builder {
        private String encryptionKey;

        public GcpResourceEncryptionParametersDto.Builder withEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
            return this;
        }

        public GcpResourceEncryptionParametersDto build() {
            return new GcpResourceEncryptionParametersDto(this);
        }
    }
}