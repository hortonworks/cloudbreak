package com.sequenceiq.environment.api.v1.environment.model.response;

import javax.validation.Valid;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;

import io.swagger.annotations.ApiModelProperty;

public class AzureEnvironmentParameters {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_PARAMETERS)
    private AzureResourceGroup resourceGroup;

    public AzureResourceGroup getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(AzureResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AzureResourceGroup azureResourceGroup;

        public Builder withAzureResourceGroup(AzureResourceGroup azureResourceGroup) {
            this.azureResourceGroup = azureResourceGroup;
            return this;
        }

        public AzureEnvironmentParameters build() {
            AzureEnvironmentParameters azureEnvironmentParameters = new AzureEnvironmentParameters();
            azureEnvironmentParameters.setResourceGroup(azureResourceGroup);
            return azureEnvironmentParameters;
        }
    }

}
