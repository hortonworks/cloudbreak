package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;

public class AzureInstances extends AbstractInstances {

    private AzureClient azureClient;

    public AzureInstances(long stackId, AzureClient azureClient, List<String> instances, String status) {
        super(stackId, instances, status);
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }
}
