package com.sequenceiq.cloudbreak.cloud.azure.client;

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
import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import okhttp3.JavaNetAuthenticator;

public class AzureClientCredentials {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClientCredentials.class);

    private final AzureCredentialView credentialView;

    private final LogLevel logLevel;

    private final AzureTokenCredentials azureClientCredentials;

    private final CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    private final AuthenticationContextProvider authenticationContextProvider;

    public AzureClientCredentials(AzureCredentialView credentialView, LogLevel logLevel, CBRefreshTokenClientProvider cbRefreshTokenClientProvider,
                    AuthenticationContextProvider authenticationContextProvider) {
        this.authenticationContextProvider = authenticationContextProvider;
        this.cbRefreshTokenClientProvider = cbRefreshTokenClientProvider;
        this.credentialView = credentialView;
        this.logLevel = logLevel;
        azureClientCredentials = getAzureCredentials();
    }

    public Azure getAzure() {
        return Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(logLevel)
                .authenticate(azureClientCredentials)
                .withSubscription(credentialView.getSubscriptionId());
    }

    private AzureTokenCredentials getAzureCredentials() {
        String tenantId = credentialView.getTenantId();
        String clientId = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        String subscriptionId = credentialView.getSubscriptionId();
        AzureEnvironment azureEnvironment = AzureEnvironment.AZURE;
        ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(clientId, tenantId, secretKey, azureEnvironment);
        Optional<Boolean> codeGrantFlow = Optional.ofNullable(credentialView.getCodeGrantFlow());

        AzureTokenCredentials result = applicationTokenCredentials;
        if (codeGrantFlow.orElse(Boolean.FALSE)) {
            String refreshToken = credentialView.getRefreshToken();
            if (StringUtils.isNoneEmpty(refreshToken)) {
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
        } else {
            LOGGER.info("Creating Azure credentials with application token credentials, credential: {}", credentialView.getName());
        }
        return result.withDefaultSubscriptionId(subscriptionId);
    }

    public Optional<String> getRefreshToken() {
        String refreshToken = null;
        Optional<Boolean> codeGrantFlow = Optional.ofNullable(credentialView.getCodeGrantFlow());
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
}
