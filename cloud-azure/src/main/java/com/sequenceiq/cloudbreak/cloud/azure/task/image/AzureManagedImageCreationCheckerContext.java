package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

public class AzureManagedImageCreationCheckerContext {

    private final AzureClient azureClient;

    private final String resourceGroupName;

    private final String imageName;

    public AzureManagedImageCreationCheckerContext(AzureClient azureClient, String resourceGroupName, String imageName) {
        this.azureClient = azureClient;
        this.resourceGroupName = resourceGroupName;
        this.imageName = imageName;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public String getImageName() {
        return imageName;
    }
}
