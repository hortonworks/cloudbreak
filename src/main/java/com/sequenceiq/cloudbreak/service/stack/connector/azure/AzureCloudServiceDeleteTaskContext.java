package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AzureCloudServiceDeleteTaskContext {

    private String commonName;
    private String name;
    private AzureClient azureClient;
    private Stack stack;


    public AzureCloudServiceDeleteTaskContext(String commonName, String name, Stack stack, AzureClient azureClient) {
        this.commonName = commonName;
        this.name = name;
        this.azureClient = azureClient;
        this.stack = stack;
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

    public Stack getStack() {
        return stack;
    }
}
