package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.io.File;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class AzureProvisionContextObject extends ProvisionContextObject {

    private AzureClient azureClient;
    private String commonName;
    private String emailAsFolder;
    private String osImageName;
    private String userData;

    public AzureProvisionContextObject(Long stackId, String commonName, AzureClient azureClient, String emailAsFolder, String osImageName, String userData) {
        super(stackId);
        this.azureClient = azureClient;
        this.commonName = commonName;
        this.emailAsFolder = emailAsFolder;
        this.osImageName = osImageName;
        this.userData = userData;
    }

    public String getOsImageName() {
        return osImageName;
    }

    public String getEmailAsFolder() {
        return emailAsFolder;
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

    public synchronized AzureClient getNewAzureClient(AzureCredential credential) {
        File file = new File(AzureCertificateService.getUserJksFileName(credential, emailAsFolder));
        return new AzureClient(credential.getSubscriptionId(), file.getAbsolutePath(), credential.getJks());
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
}
