package com.sequenceiq.environment.parameter.dto;

public class AzureResourceEncryptionParametersDto {
    private final String encryptionKeyUrl;

    private final String diskEncryptionSetId;

    private AzureResourceEncryptionParametersDto(AzureResourceEncryptionParametersDto.Builder builder) {
        encryptionKeyUrl = builder.encryptionKeyUrl;
        diskEncryptionSetId = builder.diskEncryptionSetId;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public static AzureResourceEncryptionParametersDto.Builder builder() {
        return new AzureResourceEncryptionParametersDto.Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParametersDto{" +
                "encryptionKeyUrl=" + encryptionKeyUrl +
                "diskEncryptionSetId=" + diskEncryptionSetId +
                '}';
    }

    public static final class Builder {
        private String encryptionKeyUrl;

        private String diskEncryptionSetId;

        public AzureResourceEncryptionParametersDto.Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public AzureResourceEncryptionParametersDto.Builder withDiskEncryptionSetId(String diskEncryptionSetId) {
            this.diskEncryptionSetId = diskEncryptionSetId;
            return this;
        }

        public AzureResourceEncryptionParametersDto build() {
            return new AzureResourceEncryptionParametersDto(this);
        }
    }
}