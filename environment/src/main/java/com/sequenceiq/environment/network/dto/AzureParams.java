package com.sequenceiq.environment.network.dto;

public class AzureParams {

    private String networkId;

    private String resourceGroupName;

    private boolean noPublicIp;

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

    public boolean isNoPublicIp() {
        return noPublicIp;
    }

    public void setNoPublicIp(boolean noPublicIp) {
        this.noPublicIp = noPublicIp;
    }

    @Override
    public String toString() {
        return "AzureParams{" +
                "networkId='" + networkId + '\'' +
                ", resourceGroupName='" + resourceGroupName + '\'' +
                ", noPublicIp=" + noPublicIp +
                '}';
    }

    public static final class AzureParamsBuilder {
        private String networkId;

        private String resourceGroupName;

        private boolean noPublicIp;

        private AzureParamsBuilder() {
        }

        public static AzureParamsBuilder anAzureParams() {
            return new AzureParamsBuilder();
        }

        public AzureParamsBuilder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public AzureParamsBuilder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public AzureParamsBuilder withNoPublicIp(boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public AzureParams build() {
            AzureParams azureParams = new AzureParams();
            azureParams.setNetworkId(networkId);
            azureParams.setResourceGroupName(resourceGroupName);
            azureParams.setNoPublicIp(noPublicIp);
            return azureParams;
        }
    }
}
