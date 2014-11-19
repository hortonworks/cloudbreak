package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;

public class AzureDescribeContextObject extends DescribeContextObject {

    private AzureClient azureClient;
    private String commonName;

    public AzureDescribeContextObject(Long stackId, String commonName, AzureClient azureClient) {
        super(stackId);
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
