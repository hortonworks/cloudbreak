package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType;

public class AzureDnsZoneDeploymentParameters {

    private final String networkId;

    private final boolean deployOnlyNetworkLinks;

    private final List<AzureManagedPrivateDnsZoneServiceType> enabledPrivateEndpointServices;

    private final String resourceGroupName;

    private final Map<String, String> tags;

    public AzureDnsZoneDeploymentParameters(String networkId, boolean deployOnlyNetworkLinks,
            List<AzureManagedPrivateDnsZoneServiceType> enabledPrivateEndpointServices, String resourceGroupName, Map<String, String> tags) {
        this.networkId = networkId;
        this.deployOnlyNetworkLinks = deployOnlyNetworkLinks;
        this.enabledPrivateEndpointServices = enabledPrivateEndpointServices;
        this.resourceGroupName = resourceGroupName;
        this.tags = tags;
    }

    public String getNetworkId() {
        return networkId;
    }

    public boolean getDeployOnlyNetworkLinks() {
        return deployOnlyNetworkLinks;
    }

    public List<AzureManagedPrivateDnsZoneServiceType> getEnabledPrivateEndpointServices() {
        return enabledPrivateEndpointServices;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzureDnsZoneDeploymentParameters.class.getSimpleName() + "[", "]")
                .add("networkId='" + networkId + "'")
                .add("deployOnlyNetworkLinks=" + deployOnlyNetworkLinks)
                .add("enabledPrivateEndpointServices=" + enabledPrivateEndpointServices)
                .add("resourceGroupName='" + resourceGroupName + "'")
                .add("tags=" + tags)
                .toString();
    }
}