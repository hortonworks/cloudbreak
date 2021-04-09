package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import javax.validation.constraints.Pattern;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureResourceEncryptionV1Parameters")
public class AzureResourceEncryptionParameters {

    @ApiModelProperty(EnvironmentModelDescription.ENCRYPTION_KEY_URL)
    @Pattern(regexp = "^https:\\/\\/[a-zA-Z-][0-9a-zA-Z-]*\\.vault\\.azure\\.net\\/keys\\/[0-9a-zA-Z-]+\\/[0-9A-Za-z]+",
            message = "It should be of format 'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'" +
                    "keyName can only contain alphanumeric characters and dashes." +
                    "keyVersion can only contain alphanumeric characters.")
    private String encryptionKeyUrl;

    @ApiModelProperty(EnvironmentModelDescription.DISK_ENCRYPTION_SET_ID)
    private String diskEncryptionSetId;

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
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
                "encryptionKeyUrl=" + encryptionKeyUrl +
                "diskEncryptionSetId=" + diskEncryptionSetId +
                '}';
    }

    public static class Builder {

        private String encryptionKeyUrl;

        private String diskEncryptionSetId;

        public Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public Builder withDiskEncryptionSetId(String diskEncryptionSetId) {
            this.diskEncryptionSetId = diskEncryptionSetId;
            return this;
        }

        public AzureResourceEncryptionParameters build() {
            AzureResourceEncryptionParameters resourceEncryptionParameters = new AzureResourceEncryptionParameters();
            resourceEncryptionParameters.setEncryptionKeyUrl(encryptionKeyUrl);
            resourceEncryptionParameters.setDiskEncryptionSetId(diskEncryptionSetId);
            return resourceEncryptionParameters;
        }
    }
}
