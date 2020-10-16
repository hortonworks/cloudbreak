package com.sequenceiq.cloudbreak.cloud.azure.task.dnszone;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

public class AzureDnsZoneCreationCheckerContext {

    private final AzureClient azureClient;

    private final String resourceGroupName;

    private final String deploymentName;

    private final String networkId;

    private final List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices;

    public AzureDnsZoneCreationCheckerContext(AzureClient azureClient, String resourceGroupName, String deploymentName, String networkId,
            List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices) {
        this.azureClient = azureClient;
        this.resourceGroupName = resourceGroupName;
        this.deploymentName = deploymentName;
        this.networkId = networkId;
        this.enabledPrivateEndpointServices = enabledPrivateEndpointServices;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public List<AzurePrivateDnsZoneServiceEnum> getEnabledPrivateEndpointServices() {
        return enabledPrivateEndpointServices;
    }
}
