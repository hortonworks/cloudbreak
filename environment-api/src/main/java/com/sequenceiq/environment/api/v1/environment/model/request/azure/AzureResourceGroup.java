package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureResourceGroupV1Parameters")
public class AzureResourceGroup implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.EXISTING_RESOURCE_GROUP_NAME)
    private String name;

    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_USAGE)
    private ResourceGroupUsage resourceGroupUsage;

    public String getName() {
        return name;
    }

    public ResourceGroupUsage getResourceGroupUsage() {
        return resourceGroupUsage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceGroupUsage(ResourceGroupUsage resourceGroupUsage) {
        this.resourceGroupUsage = resourceGroupUsage;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureResourceGroup{" +
                "name='" + name + '\'' +
                ", resourceGroupUsage=" + resourceGroupUsage +
                '}';
    }

    public static class Builder {
        private String name;

        private ResourceGroupUsage resourceGroupUsage;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withResourceGroupUsage(ResourceGroupUsage resourceGroupUsage) {
            this.resourceGroupUsage = resourceGroupUsage;
            return this;
        }

        public AzureResourceGroup build() {
            AzureResourceGroup azureResourceGroup = new AzureResourceGroup();
            azureResourceGroup.setName(name);
            azureResourceGroup.setResourceGroupUsage(resourceGroupUsage);
            return azureResourceGroup;
        }
    }
}
