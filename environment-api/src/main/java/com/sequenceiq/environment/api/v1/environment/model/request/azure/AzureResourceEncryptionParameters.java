package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import javax.validation.constraints.Pattern;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureResourceEncryptionV1Parameters")
public class AzureResourceEncryptionParameters {

    @ApiModelProperty(EnvironmentModelDescription.KEY_URL)
    @Pattern(regexp = "^https:\\/\\/[a-zA-Z-][0-9a-zA-Z-]*\\.vault\\.azure\\.net\\/keys\\/[0-9a-zA-Z-]+\\/[0-9A-Za-z]+",
            message = "It should be of format 'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'" +
                    "keyName can only contain alphanumeric characters and dashes." +
                    "keyVersion can only contain alphanumeric characters.")
    private String keyUrl;

    public String getKeyUrl() {
        return keyUrl;
    }

    public void setKeyUrl(String keyUrl) {
        this.keyUrl = keyUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceEncryptionParameters{" +
                "keyUrl=" + keyUrl +
                '}';
    }

    public static class Builder {

        private String keyUrl;

        public Builder withKeyUrl(String keyUrl) {
            this.keyUrl = keyUrl;
            return this;
        }

        public AzureResourceEncryptionParameters build() {
            AzureResourceEncryptionParameters resourceEncryptionParameters = new AzureResourceEncryptionParameters();
            resourceEncryptionParameters.setKeyUrl(keyUrl);
            return resourceEncryptionParameters;
        }
    }
}
