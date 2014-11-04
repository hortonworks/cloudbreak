package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloud.azure.client.AzureClient;

public class AzureDiskRemoveReadyPollerObject {

    private String commonName;
    private String name;
    private AzureClient azureClient;
    private Long stackId;

    public AzureDiskRemoveReadyPollerObject(String commonName, String name, Long stackId, AzureClient azureClient) {
        this.commonName = commonName;
        this.name = name;
        this.azureClient = azureClient;
        this.stackId = stackId;
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

    public Long getStackId() {
        return stackId;
    }
}
