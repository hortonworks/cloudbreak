package com.sequenceiq.it.cloudbreak.util.azure.azurevm.client;

import static com.azure.core.http.policy.HttpLogDetailLevel.BODY_AND_HEADERS;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties.Credential;

@Configuration
public class AzureClientConfiguration {

    private static final int MAX_RESPONSE_HEADER_SIZE = 16384;

    @Inject
    private AzureProperties azureProperties;

    @Bean
    public AzureResourceManager getAzure() {
        HttpClient httpClient = new NettyAsyncHttpClientBuilder(reactor.netty.http.client.HttpClient.create()
                .httpResponseDecoder(spec -> spec.maxHeaderSize(MAX_RESPONSE_HEADER_SIZE)))
                .build();
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
                .withHttpClient(httpClient)
                .authenticate(clientSecretCredential, azureProfile)
                .withSubscription(credential.getSubscriptionId());
    }
}
