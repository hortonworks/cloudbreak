package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.io.File;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

public class AzureStartStopContextObject extends StartStopContextObject {

    private AzureClient azureClient;
    private String emailAsFolder;

    public AzureStartStopContextObject(Stack stack, AzureClient azureClient, String emailAsFolder) {
        super(stack);
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
