package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import javax.validation.constraints.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureResourceEncryptionV1Parameters")
public class AzureResourceEncryptionParameters {

    @VisibleForTesting
    static final String ENCRYPTION_KEY_URL_INVALID_MSG = "It should be of format 'https://<vaultName><dnsSuffix>/keys/<keyName>/<keyVersion>'. " +
            "<vaultName> may only contain alphanumeric characters and dashes. " +
            "<dnsSuffix> shall be either of the following: " +
            "\".vault.azure.net\", \".vault.azure.cn\", \".vault.usgovcloudapi.net\", \".vault.microsoftazure.de\". " +
            "<keyName> may only contain alphanumeric characters and dashes. " +
            "<keyVersion> may only contain alphanumeric characters.";

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY_URL)
    @Pattern(regexp =
            "^https://[a-zA-Z-][0-9a-zA-Z-]*\\.vault\\.(azure\\.net|azure\\.cn|usgovcloudapi\\.net|microsoftazure\\.de)/keys/[0-9a-zA-Z-]+/[0-9A-Za-z]+",
            message = ENCRYPTION_KEY_URL_INVALID_MSG)
    private String encryptionKeyUrl;

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
    private String encryptionKeyResourceGroupName;

    @ApiModelProperty(EnvironmentModelDescription.DISK_ENCRYPTION_SET_ID)
    private String diskEncryptionSetId;

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    public String getEncryptionKeyResourceGroupName() {
        return encryptionKeyResourceGroupName;
    }

    public void setEncryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
        this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public void setDiskEncryptionSetId(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParameters{" +
                "encryptionKeyUrl='" + encryptionKeyUrl + '\'' +
                ", encryptionKeyResourceGroupName='" + encryptionKeyResourceGroupName + '\'' +
                ", diskEncryptionSetId='" + diskEncryptionSetId + '\'' +
                '}';
    }

    public static class Builder {

        private String encryptionKeyUrl;

        private String encryptionKeyResourceGroupName;

        private String diskEncryptionSetId;

        private Builder() {
        }

        public Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public Builder withEncryptionKeyResourceGroupName(String encryptionKeyResourceGroupName) {
            this.encryptionKeyResourceGroupName = encryptionKeyResourceGroupName;
            return this;
        }

        public Builder withDiskEncryptionSetId(String diskEncryptionSetId) {
            this.diskEncryptionSetId = diskEncryptionSetId;
            return this;
        }

        public AzureResourceEncryptionParameters build() {
            AzureResourceEncryptionParameters resourceEncryptionParameters = new AzureResourceEncryptionParameters();
            resourceEncryptionParameters.setEncryptionKeyUrl(encryptionKeyUrl);
            resourceEncryptionParameters.setEncryptionKeyResourceGroupName(encryptionKeyResourceGroupName);
            resourceEncryptionParameters.setDiskEncryptionSetId(diskEncryptionSetId);
            return resourceEncryptionParameters;
        }

    }

}
