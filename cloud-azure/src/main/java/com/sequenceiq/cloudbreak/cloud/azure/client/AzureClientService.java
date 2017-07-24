package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AzureClientService {

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) throws IOException {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureClient azureClient = getClient(cloudCredential);
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        return authenticatedContext;
    }

    public AzureClient getClient(CloudCredential cloudCredential) throws IOException {
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        return new AzureClient(azureCredentialView.getTenantId(), azureCredentialView.getAccessKey(),
                azureCredentialView.getSecretKey(), azureCredentialView.getSubscriptionId());
    }


}
