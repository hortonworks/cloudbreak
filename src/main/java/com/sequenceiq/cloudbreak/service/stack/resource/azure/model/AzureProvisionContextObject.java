package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class AzureProvisionContextObject extends ProvisionContextObject {

    private AzureClient azureClient;
    private String commonName;
    private String osImageName;
    private String userData;

    public AzureProvisionContextObject(Long stackId, String commonName, AzureClient azureClient, String osImageName, String userData) {
        super(stackId);
        this.azureClient = azureClient;
        this.commonName = commonName;
        this.osImageName = osImageName;
        this.userData = userData;
    }

    public String getOsImageName() {
        return osImageName;
    }

    public String getUserData() {
        return userData;
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

}
