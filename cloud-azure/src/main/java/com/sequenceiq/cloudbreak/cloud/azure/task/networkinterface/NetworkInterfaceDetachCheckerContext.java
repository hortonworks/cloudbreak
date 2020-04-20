package com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

public class NetworkInterfaceDetachCheckerContext {
    private AzureClient azureClient;

    private String resourceGroupName;

    private Collection<String> networkInterfaceNames;

    public NetworkInterfaceDetachCheckerContext(AzureClient azureClient, String resourceGroupName, Collection<String> networkInterfaceNames) {
        this.azureClient = azureClient;
        this.resourceGroupName = resourceGroupName;
        this.networkInterfaceNames = networkInterfaceNames;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public Collection<String> getNetworkInterfaceNames() {
        return networkInterfaceNames;
    }
}
