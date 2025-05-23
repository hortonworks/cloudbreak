package com.sequenceiq.cloudbreak.cloud.azure.view;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.model.PrivateEndpointType;

public class AzureNetworkView {

    private static final String SUBNETS = "subnets";

    private static final String SUBNET_FOR_PRIVATE_ENDPOINT = "subnetForPrivateEndpoint";

    private static final String ENDPOINT_TYPE = "endpointType";

    private static final String EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID = "existingDatabasePrivateDnsZoneId";

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

    public List<String> getSubnetList() {
        return Arrays.asList(network.getStringParameter(SUBNETS).split(","));
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

    public PrivateEndpointType getEndpointType() {
        return PrivateEndpointType.safeValueOf(network.getStringParameter(ENDPOINT_TYPE));
    }

    public String getSubnetIdForPrivateEndpoint() {
        return network.getStringParameter(SUBNET_FOR_PRIVATE_ENDPOINT);
    }

    public String getExistingDatabasePrivateDnsZoneId() {
        return network.getStringParameter(EXISTING_DATABASE_PRIVATE_DNS_ZONE_ID);
    }

    public String getFlexibleServerDelegatedSubnetId() {
        return network.getStringParameter(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID);
    }

}