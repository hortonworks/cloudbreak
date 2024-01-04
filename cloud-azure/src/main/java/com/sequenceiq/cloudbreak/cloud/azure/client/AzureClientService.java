package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.common.api.credential.AppCertificateStatus;

@Service
public class AzureClientService {

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Inject
    private AzureHttpClientConfigurer azureHttpClientConfigurer;

    @Inject
    private AzureListResultFactory azureListResultFactory;

    @Inject
    @Qualifier("azureClientThreadPool")
    private ExecutorService mdcCopyingThreadPoolExecutor;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureCredentialView credentialView = new AzureCredentialView(cloudCredential);
        if (!credentialIsInKeyGeneratedStatus(credentialView)) {
            AzureClient azureClient = getClient(cloudContext, cloudCredential);
            authenticatedContext.putParameter(AzureClient.class, azureClient);
        }
        return authenticatedContext;
    }

    public AzureClient getClient(CloudCredential cloudCredential) {
        return getClient(null, cloudCredential);
    }

    public AzureClient getClient(CloudContext cloudContext, CloudCredential cloudCredential) {
        return getClient(cloudContext, new AzureCredentialView(cloudCredential));
    }

    private AzureClient getClient(CloudContext cloudContext, AzureCredentialView credentialView) {
        AzureClientFactory azureClientFactory = new AzureClientFactory(cloudContext, credentialView, mdcCopyingThreadPoolExecutor,
                azureHttpClientConfigurer);
        return new AzureClient(azureClientFactory, azureExceptionHandler, azureListResultFactory);
    }

    private boolean credentialIsInKeyGeneratedStatus(AzureCredentialView credentialView) {
        return AppAuthenticationType.CERTIFICATE.name().equals(credentialView.getAuthenticationType())
                && AppCertificateStatus.KEY_GENERATED.name().equals(credentialView.getStatus());
    }
}
