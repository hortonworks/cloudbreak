package com.sequenceiq.environment.api.v1.environment.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkAzureV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkAzureParams {

    @Size(max = 255)
    @ApiModelProperty(value = EnvironmentModelDescription.AZURE_NETWORK_ID, required = true)
    private String networkId;

    @Size(max = 255)
    @ApiModelProperty(value = EnvironmentModelDescription.AZURE_RESOURCE_GROUP_NAME, required = true)
    private String resourceGroupName;

    @NotNull
    @ApiModelProperty(EnvironmentModelDescription.AZURE_NO_PUBLIC_IP)
    private Boolean noPublicIp;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public Boolean getNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(Boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    public static final class EnvironmentNetworkAzureParamsBuilder {
        private String networkId;

        private String resourceGroupName;

        private Boolean noPublicIp;

        private EnvironmentNetworkAzureParamsBuilder() {
        }

        public static EnvironmentNetworkAzureParamsBuilder anEnvironmentNetworkAzureParams() {
            return new EnvironmentNetworkAzureParamsBuilder();
        }

        public EnvironmentNetworkAzureParamsBuilder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public EnvironmentNetworkAzureParamsBuilder withNoPublicIp(Boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public EnvironmentNetworkAzureParams build() {
            EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
            environmentNetworkAzureParams.setNetworkId(networkId);
            environmentNetworkAzureParams.setResourceGroupName(resourceGroupName);
            environmentNetworkAzureParams.setNoPublicIp(noPublicIp);
            return environmentNetworkAzureParams;
        }
    }

}
