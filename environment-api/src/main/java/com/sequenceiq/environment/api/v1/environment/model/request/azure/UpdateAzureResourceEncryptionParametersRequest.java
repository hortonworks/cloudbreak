package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateAzureEncryptionParametersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateAzureResourceEncryptionParametersRequest implements Serializable {

    @Schema(description = EnvironmentModelDescription.RESOURCE_ENCRYPTION_PARAMETERS)
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
