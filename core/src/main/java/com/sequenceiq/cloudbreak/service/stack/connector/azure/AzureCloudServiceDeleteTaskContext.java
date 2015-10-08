package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureCloudServiceDeleteTaskContext extends StackContext {

    private String name;
    private AzureClient azureClient;

    public AzureCloudServiceDeleteTaskContext(String name, Stack stack, AzureClient azureClient) {
        super(stack);
        this.name = name;
        this.azureClient = azureClient;
    }

    public String getName() {
        return name;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

}
