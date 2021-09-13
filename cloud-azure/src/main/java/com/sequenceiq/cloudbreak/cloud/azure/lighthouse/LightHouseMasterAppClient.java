package com.sequenceiq.cloudbreak.cloud.azure.lighthouse;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.client.AuthenticationContextProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientCredentials;
import com.sequenceiq.cloudbreak.cloud.azure.client.CBRefreshTokenClientProvider;
import com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureRoleManager;
import com.sequenceiq.cloudbreak.cloud.azure.tracing.AzureOkHttp3TracingInterceptor;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;

@Component
public class LightHouseMasterAppClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightHouseMasterAppClient.class);

    @Value("${cb.azure.loglevel:BASIC}")
    private LogLevel logLevel;

    @Value("${cb.azure.lighthouse.masterapp.subscriptionId}")
    private String lighthouseMasterAppSubscriptionId;

    @Value("${cb.azure.lighthouse.masterapp.tenantId}")
    private String lighthouseMasterAppTenantId;

    @Value("${cb.azure.lighthouse.masterapp.accessKey}")
    private String lighthouseMasterAppAccessKey;

    @Value("${cb.azure.lighthouse.masterapp.secretKey}")
    private String lighthouseMasterAppSecretKey;

    @Inject
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Inject
    private AuthenticationContextProvider authenticationContextProvider;

    @Inject
    private AzureOkHttp3TracingInterceptor tracingInterceptor;

    @Inject
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    @Inject
    private AzureRoleManager azureRoleManager;

    private AzureClient getClient() {
        AzureCredentialView azureCredentialView = new AzureCredentialView(
                lighthouseMasterAppSubscriptionId,
                lighthouseMasterAppTenantId,
                lighthouseMasterAppAccessKey,
                lighthouseMasterAppSecretKey
        );
        AzureClientCredentials azureClientCredentials = new AzureClientCredentials(azureCredentialView, logLevel, cbRefreshTokenClientProvider,
                authenticationContextProvider, tracingInterceptor);
        return new AzureClient(azureClientCredentials, azureAuthExceptionHandler);
    }

    public void createLightHouseCutomerAppInClouderaTenant() {
        LOGGER.info("sdfsdfdsf");
        //getClient().createServicePrincipal();
        //azureRoleManager.assignRole();
    }

}
