package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AzureInstances extends AbstractInstances {

    private AzureClient azureClient;

    public AzureInstances(Stack stack, AzureClient azureClient, List<String> instances, String status) {
        super(stack, instances, status);
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }
}
