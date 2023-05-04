package com.sequenceiq.it.cloudbreak.util.azure.azurevm.client;

import static com.azure.core.http.policy.HttpLogDetailLevel.BODY_AND_HEADERS;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties.Credential;

@Configuration
public class AzureClientConfiguration {

    @Inject
    private AzureProperties azureProperties;

    @Bean
    public AzureResourceManager getAzure() {
            Credential credential = azureProperties.getCredential();
            ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                    .clientId(credential.getAppId())
                    .tenantId(credential.getTenantId())
                    .clientSecret(credential.getAppPassword())
                    .build();
        AzureProfile azureProfile = new AzureProfile(credential.getTenantId(), credential.getSubscriptionId(), AzureEnvironment.AZURE);
        return AzureResourceManager
                    .configure()
                    .withLogLevel(BODY_AND_HEADERS)
                    .authenticate(clientSecretCredential, azureProfile)
                    .withSubscription(credential.getSubscriptionId());
    }
}
