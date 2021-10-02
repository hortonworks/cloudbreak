package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "UpdateAzureEncryptionParametersV1Request")
public class UpdateAzureResourceEncryptionParametersRequest implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_ENCRYPTION_PARAMETERS)
    private AzureResourceEncryptionParameters azureResourceEncryptionParameters;

    public AzureResourceEncryptionParameters getAzureResourceEncryptionParameters() {
        return azureResourceEncryptionParameters;
    }

    public void setAzureResourceEncryptionParameters(AzureResourceEncryptionParameters azureResourceEncryptionParameters) {
        this.azureResourceEncryptionParameters = azureResourceEncryptionParameters;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "UpdateAzureResourceEncryptionParametersRequest{" +
                "AzureResourceEncryptionParameters='" + azureResourceEncryptionParameters + '\'' +
                '}';
    }

    public static class Builder {
        private AzureResourceEncryptionParameters azureResourceEncryptionParameters;

        private Builder() {
        }

        public Builder withAzureResourceEncryptionParameters(AzureResourceEncryptionParameters azureResourceEncryptionParameters) {
            this.azureResourceEncryptionParameters = azureResourceEncryptionParameters;
            return this;
        }

        public UpdateAzureResourceEncryptionParametersRequest build() {
            UpdateAzureResourceEncryptionParametersRequest updateAzureResourceEncryptionParametersRequest = new UpdateAzureResourceEncryptionParametersRequest();
            updateAzureResourceEncryptionParametersRequest.setAzureResourceEncryptionParameters(azureResourceEncryptionParameters);
            return updateAzureResourceEncryptionParametersRequest;
        }
    }
}
