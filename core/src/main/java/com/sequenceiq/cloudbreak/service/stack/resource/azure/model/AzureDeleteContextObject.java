package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;

public class AzureDeleteContextObject extends DeleteContextObject {

    private AzureClient azureClient;

    public AzureDeleteContextObject(Long stackId, AzureClient azureClient) {
        super(stackId);
        this.azureClient = azureClient;
    }

    public AzureDeleteContextObject(Long stackId, AzureClient azureClient, List<Resource> decommissionResources) {
        super(stackId, decommissionResources);
        this.azureClient = azureClient;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }

}
