package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class AzureClientService {

    @Value("${cb.azure.loglevel:BASIC}")
    private LogLevel logLevel;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureClient azureClient = getClient(cloudCredential);
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        return authenticatedContext;
    }

    public AzureClient getClient(CloudCredential cloudCredential) {
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        return new AzureClient(azureCredentialView.getTenantId(), azureCredentialView.getAccessKey(),
                azureCredentialView.getSecretKey(), azureCredentialView.getSubscriptionId(), logLevel);
    }
}
