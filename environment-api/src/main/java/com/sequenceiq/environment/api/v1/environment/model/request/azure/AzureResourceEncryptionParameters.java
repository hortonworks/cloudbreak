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

    public String getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParameters{" +
                "encryptionKeyUrl=" + encryptionKeyUrl +
                '}';
    }

    public static class Builder {

        private String encryptionKeyUrl;

        public Builder withEncryptionKeyUrl(String encryptionKeyUrl) {
            this.encryptionKeyUrl = encryptionKeyUrl;
            return this;
        }

        public AzureResourceEncryptionParameters build() {
            AzureResourceEncryptionParameters resourceEncryptionParameters = new AzureResourceEncryptionParameters();
            resourceEncryptionParameters.setEncryptionKeyUrl(encryptionKeyUrl);
            return resourceEncryptionParameters;
        }
    }
}
