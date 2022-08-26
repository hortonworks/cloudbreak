package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.implementation.ComputeManager;
import com.microsoft.azure.management.marketplaceordering.v2015_06_01.implementation.MarketplaceOrderingManager;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.privatednsManager;
import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.tracing.AzureOkHttp3TracingInterceptor;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.credential.AppAuthenticationType;

import okhttp3.JavaNetAuthenticator;

public class AzureClientCredentials {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientCredentials.class);

    private static final String RESOURCE_MANAGER_ENDPOINT_URL = "resourceManagerEndpointUrl";

    private final AzureCredentialView credentialView;

    private final LogLevel logLevel;

    private final AzureTokenCredentials azureClientCredentials;

    private final CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    private final AuthenticationContextProvider authenticationContextProvider;

    private final AzureOkHttp3TracingInterceptor tracingInterceptor;

    public AzureClientCredentials(AzureCredentialView credentialView, LogLevel logLevel, CBRefreshTokenClientProvider cbRefreshTokenClientProvider,
            AuthenticationContextProvider authenticationContextProvider, AzureOkHttp3TracingInterceptor tracingInterceptor) {
        this(null, credentialView, logLevel, cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor);
    }

    public AzureClientCredentials(CloudContext cloudContext, AzureCredentialView credentialView, LogLevel logLevel,
            CBRefreshTokenClientProvider cbRefreshTokenClientProvider, AuthenticationContextProvider authenticationContextProvider,
            AzureOkHttp3TracingInterceptor tracingInterceptor) {
        this.authenticationContextProvider = authenticationContextProvider;
        this.cbRefreshTokenClientProvider = cbRefreshTokenClientProvider;
        this.credentialView = credentialView;
        this.logLevel = logLevel;
        this.tracingInterceptor = tracingInterceptor;
        azureClientCredentials = getAzureCredentials(Optional.ofNullable(cloudContext)
                .map(CloudContext::getLocation)
                .map(Location::getRegion));
    }

    public Azure getAzure() {
        return Azure
                .configure()
                .withInterceptor(tracingInterceptor)
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(logLevel)
                .authenticate(azureClientCredentials)
                .withSubscription(credentialView.getSubscriptionId());
    }

    public privatednsManager getPrivateDnsManager() {
        return privatednsManager.authenticate(azureClientCredentials, credentialView.getSubscriptionId());
    }

    public privatednsManager getPrivateDnsManagerWithAnotherSubscription(String subscriptionId) {
        return privatednsManager.authenticate(azureClientCredentials, subscriptionId);
    }

    public ComputeManager getComputeManager() {
        return ComputeManager.authenticate(azureClientCredentials, credentialView.getSubscriptionId());
    }

    public MarketplaceOrderingManager getMarketplaceOrderingManager() {
        return MarketplaceOrderingManager.authenticate(azureClientCredentials, credentialView.getSubscriptionId());
    }

    AzureTokenCredentials getAzureCredentials(Optional<Region> region) {
        String subscriptionId = credentialView.getSubscriptionId();
        AzureEnvironment azureEnvironment = getAzureEnvironment(region);

        ApplicationTokenCredentials applicationTokenCredentials;
        if (AppAuthenticationType.CERTIFICATE.name().equals(credentialView.getAuthenticationType())) {
            applicationTokenCredentials = certificateBasedCredentials(azureEnvironment);
        } else {
            applicationTokenCredentials = secretBasedCredentials(azureEnvironment);
        }
        AzureTokenCredentials result = applicationTokenCredentials;

        if (credentialView.codeGrantFlow()) {
            result = delegatedTokenCredentials(azureEnvironment, applicationTokenCredentials);
        } else {
            LOGGER.info("Creating Azure credentials with application token credentials, credential: {}", credentialView.getName());
        }
        return result.withDefaultSubscriptionId(subscriptionId);
    }

    private ApplicationTokenCredentials certificateBasedCredentials(AzureEnvironment azureEnvironment) {
        LOGGER.info("Creating Azure credentials with certificate and private key: {}", credentialView.getName());
        String auth = credentialView.getPrivateKeyForCertificate() + credentialView.getCertificate();
        return new ApplicationTokenCredentials(credentialView.getAccessKey(), credentialView.getTenantId(),
                auth.getBytes(), null, azureEnvironment);
    }

    private ApplicationTokenCredentials secretBasedCredentials(AzureEnvironment azureEnvironment) {
        LOGGER.info("Creating Azure credentials with secret: {}", credentialView.getName());
        return new ApplicationTokenCredentials(credentialView.getAccessKey(), credentialView.getTenantId(),
                credentialView.getSecretKey(), azureEnvironment);
    }

    private AzureTokenCredentials delegatedTokenCredentials(AzureEnvironment azureEnvironment, ApplicationTokenCredentials applicationTokenCredentials) {
        // Probably this is a dead code at the moment, since code grant flow is not supported, I just put it into a separate method
        String tenantId = credentialView.getTenantId();
        String clientId = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        AzureTokenCredentials result;
        String refreshToken = credentialView.getRefreshToken();
        if (StringUtils.isNotEmpty(refreshToken)) {
            LOGGER.info("Creating Azure credentials for a new delegated token with refresh token, credential: {}", credentialView.getName());
            String resource = azureEnvironment.managementEndpoint();
            CBRefreshTokenClient refreshTokenClient = cbRefreshTokenClientProvider.getCBRefreshTokenClient(azureEnvironment.activeDirectoryEndpoint());
            AuthenticationResult authenticationResult = refreshTokenClient.refreshToken(tenantId, clientId, secretKey, resource, refreshToken, false);

            if (authenticationResult == null) {
                String msg = String.format("New token couldn't be obtain with refresh token for credential: %s", credentialView.getName());
                LOGGER.warn(msg);
                throw new CloudConnectorException(msg);
            }

            Map<String, AuthenticationResult> tokens = Map.of(resource, authenticationResult);
            result = new CbDelegatedTokenCredentials(applicationTokenCredentials, resource, tokens, secretKey, authenticationContextProvider,
                    cbRefreshTokenClientProvider);
        } else {
            LOGGER.info("Creating Azure credentials for a new delegated token with authorization code, credential: {}", credentialView.getName());
            String appReplyUrl = credentialView.getAppReplyUrl();
            String authorizationCode = credentialView.getAuthorizationCode();
            result = new CbDelegatedTokenCredentials(applicationTokenCredentials, appReplyUrl, authorizationCode, secretKey, authenticationContextProvider,
                    cbRefreshTokenClientProvider);
        }
        return result;
    }

    private AzureEnvironment getAzureEnvironment(Optional<Region> region) {
        AzureEnvironment azureEnvironment = new AzureEnvironment(new HashMap<>());
        azureEnvironment.endpoints().putAll(AzureEnvironment.AZURE.endpoints());
        if (region.isPresent()) {
            Map<String, String> endpoints = azureEnvironment.endpoints();
            String resourceManagerEndpoint = endpoints.get(RESOURCE_MANAGER_ENDPOINT_URL);
            String compressedRegion = compressRegion(region.get().getRegionName());
            if (!resourceManagerEndpoint.contains(compressedRegion + ".")) {
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
        }
        return azureEnvironment;
    }

    private String compressRegion(String regionName) {
        return regionName.toLowerCase().replaceAll("\\s", "");
    }

    public Optional<String> getRefreshToken() {
        String refreshToken = null;
        Optional<Boolean> codeGrantFlow = Optional.ofNullable(credentialView.codeGrantFlow());
        if (codeGrantFlow.orElse(Boolean.FALSE)) {
            CbDelegatedTokenCredentials delegatedCredentials = (CbDelegatedTokenCredentials) azureClientCredentials;
            Optional<AuthenticationResult> authenticationResult = delegatedCredentials.getTokens()
                    .values()
                    .stream()
                    .findFirst();

            if (authenticationResult.isPresent()) {
                refreshToken = authenticationResult.get().getRefreshToken();
            }
        }
        return Optional.ofNullable(refreshToken);
    }

    public Optional<String> getAccessToken() {
        try {
            return Optional.of(azureClientCredentials.getToken("https://management.core.windows.net/"));
        } catch (IOException e) {
            LOGGER.warn("Could not get access token.");
            return Optional.empty();
        }
    }

}
