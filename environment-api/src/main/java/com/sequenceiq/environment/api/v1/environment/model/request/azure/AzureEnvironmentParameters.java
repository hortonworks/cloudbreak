package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureEnvironmentV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureEnvironmentParameters implements Serializable {

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_GROUP_PARAMETERS)
    private AzureResourceGroup resourceGroup;

    @Valid
    @ApiModelProperty(EnvironmentModelDescription.RESOURCE_ENCRYPTION_PARAMETERS)
    private AzureResourceEncryptionParameters resourceEncryptionParameters;

    @ApiModelProperty(EnvironmentModelDescription.NO_OUTBOUND_LOAD_BALANCER)
    private boolean noOutboundLoadBalancer;

    public AzureResourceGroup getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(AzureResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public AzureResourceEncryptionParameters getResourceEncryptionParameters() {
        return resourceEncryptionParameters;
    }

    public void setResourceEncryptionParameters(AzureResourceEncryptionParameters resourceEncryptionParameters) {
        this.resourceEncryptionParameters = resourceEncryptionParameters;
    }

    public boolean isNoOutboundLoadBalancer() {
        return noOutboundLoadBalancer;
    }

    public void setNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
        this.noOutboundLoadBalancer = noOutboundLoadBalancer;
    }

    @Override
    public String toString() {
        return "AzureEnvironmentParameters{" +
                "resourceGroup=" + resourceGroup +
                ", resourceEncryptionParameters=" + resourceEncryptionParameters +
                ", noOutboundLoadBalancer=" + noOutboundLoadBalancer +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AzureResourceGroup azureResourceGroup;

        private AzureResourceEncryptionParameters resourceEncryptionParameters;

        private boolean noOutboundLoadBalancer;

        private Builder() {
        }

        public Builder withAzureResourceGroup(AzureResourceGroup azureResourceGroup) {
            this.azureResourceGroup = azureResourceGroup;
            return this;
        }

        public Builder withResourceEncryptionParameters(AzureResourceEncryptionParameters resourceEncryptionParameters) {
            this.resourceEncryptionParameters = resourceEncryptionParameters;
            return this;
        }

        public Builder withNoOutboundLoadBalancer(boolean noOutboundLoadBalancer) {
            this.noOutboundLoadBalancer = noOutboundLoadBalancer;
            return this;
        }

        public AzureEnvironmentParameters build() {
            AzureEnvironmentParameters azureEnvironmentParameters = new AzureEnvironmentParameters();
            azureEnvironmentParameters.setResourceGroup(azureResourceGroup);
            azureEnvironmentParameters.setResourceEncryptionParameters(resourceEncryptionParameters);
            azureEnvironmentParameters.setNoOutboundLoadBalancer(noOutboundLoadBalancer);
            return azureEnvironmentParameters;
        }
    }
}
