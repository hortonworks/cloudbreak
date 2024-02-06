package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureResourceEncryptionParametersDto.Builder.class)
public class AzureResourceEncryptionParametersDto {
    private final String encryptionKeyUrl;

    private final String encryptionKeyResourceGroupName;

    private final String diskEncryptionSetId;

    private final String userManagedIdentity;

    private final boolean enableHostEncryption;

    private AzureResourceEncryptionParametersDto(Builder builder) {
        encryptionKeyUrl = builder.encryptionKeyUrl;
        encryptionKeyResourceGroupName = builder.encryptionKeyResourceGroupName;
        diskEncryptionSetId = builder.diskEncryptionSetId;
        enableHostEncryption = builder.enableHostEncryption;
        userManagedIdentity = builder.userManagedIdentity;
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

    public String getUserManagedIdentity() {
        return userManagedIdentity;
    }

    public boolean getEnableHostEncryption() {
        return enableHostEncryption;
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
                ", enableHostEncryption=" + enableHostEncryption +
                ", userManagedIdentity=" + userManagedIdentity +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String encryptionKeyUrl;

        private String diskEncryptionSetId;

        private String encryptionKeyResourceGroupName;

        private String userManagedIdentity;

        private boolean enableHostEncryption;

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

        public AzureResourceEncryptionParametersDto.Builder withUserManagedIdentity(String userManagedIdentity) {
            this.userManagedIdentity = userManagedIdentity;
            return this;
        }

        public AzureResourceEncryptionParametersDto.Builder withEnableHostEncryption(boolean enableHostEncryption) {
            this.enableHostEncryption = enableHostEncryption;
            return this;
        }

        public AzureResourceEncryptionParametersDto build() {
            return new AzureResourceEncryptionParametersDto(this);
        }
    }
}
