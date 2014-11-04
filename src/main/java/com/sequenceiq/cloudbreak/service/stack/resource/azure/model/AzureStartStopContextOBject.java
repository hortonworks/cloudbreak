package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.io.File;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class AzureStartStopContextOBject extends StartStopContextObject {

    private AzureClient azureClient;
    private String emailAsFolder;

    public AzureStartStopContextOBject(Long stackId, AzureClient azureClient, String emailAsFolder) {
        super(stackId);
        this.azureClient = azureClient;
        this.emailAsFolder = emailAsFolder;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public String getEmailAsFolder() {
        return emailAsFolder;
    }

    public synchronized AzureClient getNewAzureClient(AzureCredential credential) {
        File file = new File(AzureCertificateService.getUserJksFileName(credential, emailAsFolder));
        return new AzureClient(credential.getSubscriptionId(), file.getAbsolutePath(), credential.getJks());
    }
}
