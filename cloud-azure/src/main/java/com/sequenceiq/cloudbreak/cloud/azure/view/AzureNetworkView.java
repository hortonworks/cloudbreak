package com.sequenceiq.cloudbreak.cloud.azure.view;

import java.util.Objects;
import java.util.StringJoiner;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AzureNetworkView {

    @VisibleForTesting
    static final String SUBNETS = "subnets";

    private String networkId;

    private String resourceGroupName;

    private boolean existingNetwork;

    private Network network;

    public AzureNetworkView() {
    }

    public AzureNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnets() {
        return network.getStringParameter(SUBNETS);
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

    public boolean getExistingNetwork() {
        return existingNetwork;
    }

    public void setExistingNetwork(boolean existingNetwork) {
        this.existingNetwork = existingNetwork;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureNetworkView.class.getSimpleName() + "[", "]")
                .add("networkId='" + networkId + "'")
                .add("resourceGroupName='" + resourceGroupName + "'")
                .add("existingNetwork=" + existingNetwork)
                .add("network=" + network)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AzureNetworkView that = (AzureNetworkView) o;
        return existingNetwork == that.existingNetwork &&
                Objects.equals(networkId, that.networkId) &&
                Objects.equals(resourceGroupName, that.resourceGroupName) &&
                Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, resourceGroupName, existingNetwork, network);
    }
}
