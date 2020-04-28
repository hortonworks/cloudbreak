package com.sequenceiq.environment.api.v1.environment.model.response;

import javax.validation.Valid;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AzureEnvironmentParametersResponse {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_PARAMETERS)
    private AzureResourceGroupResponse resourceGroup;

    public AzureResourceGroupResponse getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(AzureResourceGroupResponse resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AzureResourceGroupResponse azureResourceGroup;

        public Builder withAzureResourceGroup(AzureResourceGroupResponse azureResourceGroup) {
            this.azureResourceGroup = azureResourceGroup;
            return this;
        }

        public AzureEnvironmentParametersResponse build() {
            AzureEnvironmentParametersResponse azureEnvironmentParameters = new AzureEnvironmentParametersResponse();
            azureEnvironmentParameters.setResourceGroup(azureResourceGroup);
            return azureEnvironmentParameters;
        }
    }

}
