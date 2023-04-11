package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureResourceEncryptionParametersDto.Builder.class)
public class AzureResourceEncryptionParametersDto {
    private final String encryptionKeyUrl;

    private final String encryptionKeyResourceGroupName;

    private final String diskEncryptionSetId;

    private AzureResourceEncryptionParametersDto(Builder builder) {
        encryptionKeyUrl = builder.encryptionKeyUrl;
        encryptionKeyResourceGroupName = builder.encryptionKeyResourceGroupName;
        diskEncryptionSetId = builder.diskEncryptionSetId;
    }

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public String getEncryptionKeyResourceGroupName() {
        return encryptionKeyResourceGroupName;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParametersDto{" +
                "encryptionKeyUrl=" + encryptionKeyUrl +
                ", encryptionKeyResourceGroupName=" + encryptionKeyResourceGroupName +
                ", diskEncryptionSetId=" + diskEncryptionSetId +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String encryptionKeyUrl;

        private String diskEncryptionSetId;

        private String encryptionKeyResourceGroupName;

        private Builder() {
        }

        public AzureResourceEncryptionParametersDto.Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public AzureResourceEncryptionParametersDto.Builder withEncryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
            this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
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
