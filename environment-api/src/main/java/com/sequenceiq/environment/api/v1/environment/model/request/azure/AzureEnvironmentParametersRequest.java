package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import javax.validation.Valid;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureEnvironmentV1Parameters")
public class AzureEnvironmentParametersRequest {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_PARAMETERS)
    private AzureResourceGroupRequest resourceGroup;

    public AzureResourceGroupRequest getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(AzureResourceGroupRequest resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AzureResourceGroupRequest azureResourceGroup;

        public Builder withAzureResourceGroup(AzureResourceGroupRequest azureResourceGroup) {
            this.azureResourceGroup = azureResourceGroup;
            return this;
        }

        public AzureEnvironmentParametersRequest build() {
            AzureEnvironmentParametersRequest azureEnvironmentParameters = new AzureEnvironmentParametersRequest();
            azureEnvironmentParameters.setResourceGroup(azureResourceGroup);
            return azureEnvironmentParameters;
        }
    }
}
