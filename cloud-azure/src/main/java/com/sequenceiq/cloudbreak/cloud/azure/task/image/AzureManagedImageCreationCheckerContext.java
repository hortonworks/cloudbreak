package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;

public class AzureManagedImageCreationCheckerContext {

    private final AzureClient azureClient;

    private final AzureImageInfo azureImageInfo;

    public AzureManagedImageCreationCheckerContext(AzureImageInfo azureImageInfo, AzureClient azureClient) {
        this.azureImageInfo = azureImageInfo;
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public AzureImageInfo getAzureImageInfo() {
        return azureImageInfo;
    }
}
