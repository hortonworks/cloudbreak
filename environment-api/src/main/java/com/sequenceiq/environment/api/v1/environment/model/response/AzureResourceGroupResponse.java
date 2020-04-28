package com.sequenceiq.environment.api.v1.environment.model.response;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AzureResourceGroupResponse {
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_NAME)
    private String name;

    @ApiModelProperty(EnvironmentModelDescription.EXISTING_RESOURCE_GROUP)
    private Boolean existing;

    @ApiModelProperty(EnvironmentModelDescription.USE_SINGLE_RESOURCE_GROUP)
    private Boolean single;

    public String getName() {
        return name;
    }

    public Boolean isExisting() {
        return existing;
    }

    public Boolean isSingle() {
        return single;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExisting(Boolean existing) {
        this.existing = existing;
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

        private Boolean existing;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withSingle(Boolean single) {
            this.single = single;
            return this;
        }

        public Builder withExisting(Boolean existing) {
            this.existing = existing;
            return this;
        }

        public AzureResourceGroupResponse build() {
            AzureResourceGroupResponse azureResourceGroupResponse = new AzureResourceGroupResponse();
            azureResourceGroupResponse.setName(name);
            azureResourceGroupResponse.setSingle(single);
            azureResourceGroupResponse.setExisting(existing);
            return azureResourceGroupResponse;
        }
    }

}
