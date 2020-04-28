package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureResourceGroupV1Parameters")
public class AzureResourceGroupRequest {

    @ApiModelProperty(EnvironmentModelDescription.EXISTING_RESOURCE_GROUP_NAME)
    private String name;

    @ApiModelProperty(EnvironmentModelDescription.USE_SINGLE_RESOURCE_GROUP)
    private Boolean single;

    public String getName() {
        return name;
    }

    public Boolean isSingle() {
        return single;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSingle(Boolean single) {
        this.single = single;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;

        private Boolean single;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withSingle(Boolean single) {
            this.single = single;
            return this;
        }

        public AzureResourceGroupRequest build() {
            AzureResourceGroupRequest azureResourceGroup = new AzureResourceGroupRequest();
            azureResourceGroup.setName(name);
            azureResourceGroup.setSingle(single);
            return azureResourceGroup;
        }
    }
}
