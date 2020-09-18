package com.sequenceiq.cloudbreak.cloud.azure.client;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.tracing.AzureOkHttp3TracingInterceptor;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AzureClientService {

    @Value("${cb.azure.loglevel:BASIC}")
    private LogLevel logLevel;

    @Inject
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Inject
    private AuthenticationContextProvider authenticationContextProvider;

    @Inject
    private AzureOkHttp3TracingInterceptor tracingInterceptor;

    @Inject
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        AzureClient azureClient = getClient(cloudCredential);
        authenticatedContext.putParameter(AzureClient.class, azureClient);
        return authenticatedContext;
    }

    public AzureClient getClient(CloudCredential cloudCredential) {
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        AzureClientCredentials azureClientCredentials = new AzureClientCredentials(azureCredentialView, logLevel, cbRefreshTokenClientProvider,
                authenticationContextProvider, tracingInterceptor);
        return new AzureClient(azureClientCredentials, azureAuthExceptionHandler);
    }
}
