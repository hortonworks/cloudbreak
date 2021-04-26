package com.sequenceiq.environment.network.dto;

public class AzureParams {

    private String networkId;

    private String resourceGroupName;

    private boolean noPublicIp;

    private AzureParams(Builder builder) {
        networkId = builder.networkId;
        resourceGroupName = builder.resourceGroupName;
        noPublicIp = builder.noPublicIp;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String networkId;

        private String resourceGroupName;

        private boolean noPublicIp;

        private Builder() {
        }

        public Builder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public Builder withResourceGroupName(String resourceGroupName) {
            this.resourceGroupName = resourceGroupName;
            return this;
        }

        public Builder withNoPublicIp(boolean noPublicIp) {
            this.noPublicIp = noPublicIp;
            return this;
        }

        public AzureParams build() {
            return new AzureParams(this);
        }
    }
}
