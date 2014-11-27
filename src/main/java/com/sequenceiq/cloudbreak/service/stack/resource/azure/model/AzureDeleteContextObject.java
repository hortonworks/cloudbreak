package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.util.List;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;

public class AzureDeleteContextObject extends DeleteContextObject {

    private AzureClient azureClient;
    private String commonName;

    public AzureDeleteContextObject(Long stackId, String commonName, AzureClient azureClient) {
        super(stackId);
        this.azureClient = azureClient;
        this.commonName = commonName;
    }

    public AzureDeleteContextObject(Long stackId, String commonName, AzureClient azureClient, List<Resource> decommisionResources) {
        super(stackId, decommisionResources);
        this.azureClient = azureClient;
        this.commonName = commonName;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public synchronized AzureClient getNewAzureClient(AzureCredential credential) {
        return AzureStackUtil.createAzureClient(credential);
    }
}
