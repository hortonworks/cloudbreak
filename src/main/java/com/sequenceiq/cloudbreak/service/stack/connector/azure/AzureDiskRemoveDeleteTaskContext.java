package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.flow.StackDependentPollerObject;

public class AzureDiskRemoveDeleteTaskContext extends StackDependentPollerObject {

    private String commonName;
    private String name;
    private AzureClient azureClient;

    public AzureDiskRemoveDeleteTaskContext(String commonName, String name, Stack stack, AzureClient azureClient) {
        super(stack);
        this.commonName = commonName;
        this.name = name;
        this.azureClient = azureClient;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getName() {
        return name;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

}
