package com.sequenceiq.it.cloudbreak.util.azure.azurevm.client;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties.Credential;

import okhttp3.JavaNetAuthenticator;

@Configuration
public class AzureClientConfiguration {

    @Inject
    private AzureProperties azureProperties;

    @Bean
    public Azure getAzure() {
            Credential credential = azureProperties.getCredential();
            AzureTokenCredentials azureTokenCredentials =
                    new ApplicationTokenCredentials(credential.getAppId(), credential.getTenantId(), credential.getAppPassword(), AzureEnvironment.AZURE);
            return Azure
                    .configure()
                    .withProxyAuthenticator(new JavaNetAuthenticator())
                    .withLogLevel(LogLevel.BODY_AND_HEADERS)
                    .authenticate(azureTokenCredentials)
                    .withSubscription(credential.getSubscriptionId());
    }
}
