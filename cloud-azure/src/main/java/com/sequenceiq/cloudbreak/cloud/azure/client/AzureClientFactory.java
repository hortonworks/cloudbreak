package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.client.traits.HttpTrait;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.implementation.IdentityClientBuilder;
import com.azure.identity.implementation.IdentityClientOptions;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.marketplaceordering.MarketplaceOrderingManager;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.privatedns.PrivateDnsZoneManager;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.credential.AppAuthenticationType;

public class AzureClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientFactory.class);

    private static final String RESOURCE_MANAGER_ENDPOINT_URL = "resourceManagerEndpointUrl";

    private final AzureCredentialView credentialView;

    private final TokenCredential azureCredential;

    private final AzureEnvironment azureEnvironment;

    private final AzureProfile azureProfile;

    private final ExecutorService mdcCopyingThreadPoolExecutor;

    private final AzureHttpClientConfigurer azureHttpClientConfigurer;

    public AzureClientFactory(
            CloudContext cloudContext,
            AzureCredentialView credentialView,
            ExecutorService mdcCopyingThreadPoolExecutor,
            AzureHttpClientConfigurer azureHttpClientConfigurer) {
        this.credentialView = credentialView;
        azureEnvironment = getAzureEnvironment(Optional.ofNullable(cloudContext)
                .map(CloudContext::getLocation)
                .map(Location::getRegion));
        azureProfile = new AzureProfile(credentialView.getTenantId(), credentialView.getSubscriptionId(), azureEnvironment);
        this.mdcCopyingThreadPoolExecutor = mdcCopyingThreadPoolExecutor;
        this.azureHttpClientConfigurer = azureHttpClientConfigurer;
        azureCredential = getAzureCredentials();
    }

    public AzureEnvironment getAzureEnvironment() {
        return azureEnvironment;
    }

    public AzureResourceManager getAzureResourceManager() {
        return azureHttpClientConfigurer.configureDefault(AzureResourceManager.configure())
                .authenticate(azureCredential, azureProfile)
                .withDefaultSubscription();
    }

    public AzureResourceManager getAzureResourceManager(String subscriptionId) {
        return azureHttpClientConfigurer.configureDefault(AzureResourceManager.configure())
                .authenticate(azureCredential, new AzureProfile(credentialView.getTenantId(), subscriptionId, azureEnvironment))
                .withDefaultSubscription();
    }

    public <T extends HttpTrait<T>> T configureDefault(T configurable) {
        return azureHttpClientConfigurer.configureDefault(configurable);
    }

    public PrivateDnsZoneManager getPrivateDnsManager() {
        return azureHttpClientConfigurer.configureDefault(PrivateDnsZoneManager.configure()).authenticate(azureCredential, azureProfile);
    }

    public PrivateDnsZoneManager getPrivateDnsManagerWithAnotherSubscription(String subscriptionId) {
        return azureHttpClientConfigurer.configureDefault(PrivateDnsZoneManager.configure())
                .authenticate(azureCredential, new AzureProfile(credentialView.getTenantId(), subscriptionId, azureEnvironment));
    }

    public ComputeManager getComputeManager() {
        return azureHttpClientConfigurer.configureDefault(ComputeManager.configure()).authenticate(azureCredential, azureProfile);
    }

    public MarketplaceOrderingManager getMarketplaceOrderingManager() {
        return azureHttpClientConfigurer.configureDefault(MarketplaceOrderingManager.configure()).authenticate(azureCredential, azureProfile);
    }

    public PostgreSqlManager getPostgreSqlManager() {
        return azureHttpClientConfigurer.configureDefault(PostgreSqlManager.configure()).authenticate(azureCredential, azureProfile);
    }

    public Optional<String> getAccessToken() {
        return Optional.of(azureCredential.getTokenSync(new TokenRequestContext().addScopes("https://management.azure.com//.default")).getToken());
    }

    private TokenCredential getAzureCredentials() {
        if (AppAuthenticationType.CERTIFICATE.name().equals(credentialView.getAuthenticationType())) {
            return newCertificateCredential();
        } else {
            return newSecretCredential();
        }
    }

    public String getAccessKey() {
        return credentialView.getAccessKey();
    }

    private TokenCredential newCertificateCredential() {
        LOGGER.info("Creating Azure credentials with certificate and private key: {}", credentialView.getName());
        Objects.requireNonNull(credentialView.getCertificate(), "'certificate' cannot be null.");
        Objects.requireNonNull(credentialView.getPrivateKeyForCertificate(), "'privateKey' cannot be null.");
        IdentityClientOptions identityClientOptions = new IdentityClientOptions()
                .setExecutorService(mdcCopyingThreadPoolExecutor)
                .setHttpClient(azureHttpClientConfigurer.newHttpClient());
        AzureQuartzRetryUtils.reconfigureAzureClientIfNeeded(identityClientOptions::setMaxRetry);
        return new CloudbreakClientCertificateCredential(
                new IdentityClientBuilder()
                        .tenantId(credentialView.getTenantId())
                        .clientId(credentialView.getAccessKey())
                        .certificate(new ByteArrayInputStream((credentialView.getPrivateKeyForCertificate() + credentialView.getCertificate()).getBytes()))
                        .identityClientOptions(identityClientOptions)
                        .build());
    }

    private TokenCredential newSecretCredential() {
        LOGGER.info("Creating Azure credentials with secret: {}", credentialView.getName());
        ClientSecretCredentialBuilder clientSecretCredentialBuilder = new ClientSecretCredentialBuilder()
                .clientId(credentialView.getAccessKey())
                .tenantId(credentialView.getTenantId())
                .clientSecret(credentialView.getSecretKey())
                .httpClient(azureHttpClientConfigurer.newHttpClient())
                .executorService(mdcCopyingThreadPoolExecutor);
        AzureQuartzRetryUtils.reconfigureAzureClientIfNeeded(clientSecretCredentialBuilder::maxRetry);
        return clientSecretCredentialBuilder.build();
    }

    private AzureEnvironment getAzureEnvironment(Optional<Region> region) {
        if (region.isEmpty()) {
            return AzureEnvironment.AZURE;
        }
        AzureEnvironment azureEnvironment = new AzureEnvironment(new HashMap<>());
        azureEnvironment.getEndpoints().putAll(AzureEnvironment.AZURE.getEndpoints());
        Map<String, String> endpoints = azureEnvironment.getEndpoints();
        String resourceManagerEndpoint = endpoints.get(RESOURCE_MANAGER_ENDPOINT_URL);
        String compressedRegion = compressRegion(region.get().getRegionName());
        if (!resourceManagerEndpoint.contains(compressedRegion + '.')) {
            try {
                URL resourceManagerEndpointUrl = new URL(resourceManagerEndpoint);

                String regionAwareUrl = String.format("%s://%s.%s",
                        resourceManagerEndpointUrl.getProtocol(),
                        compressedRegion,
                        resourceManagerEndpointUrl.getHost());
                endpoints.put(RESOURCE_MANAGER_ENDPOINT_URL, regionAwareUrl);
                azureEnvironment = new AzureEnvironment(endpoints);
            } catch (MalformedURLException e) {
                LOGGER.info("Invalid URL format {}, this should not happen, we fallback to global endpoint.", resourceManagerEndpoint);
            }
        }
        return azureEnvironment;
    }

    private String compressRegion(String regionName) {
        return regionName.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
    }

}
