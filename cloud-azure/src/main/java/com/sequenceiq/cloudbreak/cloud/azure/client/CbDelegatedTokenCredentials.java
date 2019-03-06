package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;

public class CbDelegatedTokenCredentials extends AzureTokenCredentials {

    /** A mapping from resource endpoint to its cached access token. */
    private Map<String, AuthenticationResult> tokens;

    private String redirectUrl;

    private String authorizationCode;

    private ApplicationTokenCredentials applicationCredentials;

    private CBRefreshTokenClient cbRefreshTokenClient;

    private String clientSecret;

    private AuthenticationContextProvider authenticationContextProvider;

    public CbDelegatedTokenCredentials(ApplicationTokenCredentials applicationCredentials, String redirectUrl, Map<String, AuthenticationResult> tokens,
            String clientSecret, AuthenticationContextProvider authenticationContextProvider, CBRefreshTokenClientProvider cbRefreshTokenClientProvider) {
        super(applicationCredentials.environment(), applicationCredentials.domain());
        this.authenticationContextProvider = authenticationContextProvider;
        this.tokens = new ConcurrentHashMap<>(tokens);
        this.redirectUrl = redirectUrl;
        cbRefreshTokenClient = cbRefreshTokenClientProvider.getCBRefreshTokenClient(applicationCredentials.environment().activeDirectoryEndpoint());
        this.clientSecret = clientSecret;
        this.applicationCredentials = applicationCredentials;
    }

    /**
     * Initializes a new instance of the DelegatedTokenCredentials.
     *
     * @param applicationCredentials the credentials representing a service principal
     * @param redirectUrl the URL to redirect to after authentication in Active Directory
     */
    public CbDelegatedTokenCredentials(ApplicationTokenCredentials applicationCredentials, String redirectUrl,
                    AuthenticationContextProvider authenticationContextProvider, CBRefreshTokenClientProvider cbRefreshTokenClientProvider) {
        super(applicationCredentials.environment(), applicationCredentials.domain());
        this.authenticationContextProvider = authenticationContextProvider;
        this.applicationCredentials = applicationCredentials;
        tokens = new ConcurrentHashMap<>();
        this.redirectUrl = redirectUrl;
        cbRefreshTokenClient = cbRefreshTokenClientProvider.getCBRefreshTokenClient(applicationCredentials.environment().activeDirectoryEndpoint());
    }

    /**
     * Initializes a new instance of the DelegatedTokenCredentials, with a pre-acquired oauth2 authorization code.
     *
     * @param applicationCredentials the credentials representing a service principal
     * @param redirectUrl the URL to redirect to after authentication in Active Directory
     * @param authorizationCode the oauth2 authorization code
     */
    public CbDelegatedTokenCredentials(ApplicationTokenCredentials applicationCredentials, String redirectUrl, String authorizationCode, String clientSecret,
                    AuthenticationContextProvider authenticationContextProvider, CBRefreshTokenClientProvider cbRefreshTokenClientProvider) {
        super(applicationCredentials.environment(), applicationCredentials.domain());
        this.authenticationContextProvider = authenticationContextProvider;
        tokens = new ConcurrentHashMap<>();
        this.redirectUrl = redirectUrl;
        this.authorizationCode = authorizationCode;
        cbRefreshTokenClient = cbRefreshTokenClientProvider.getCBRefreshTokenClient(applicationCredentials.environment().activeDirectoryEndpoint());
        this.clientSecret = clientSecret;
        this.applicationCredentials = applicationCredentials;
    }

    /**
     * Creates a new instance of the DelegatedTokenCredentials from an auth file.
     *
     * @param authFile The credentials based on the file
     * @param redirectUrl the URL to redirect to after authentication in Active Directory
     * @return a new delegated token credentials
     * @throws IOException exception thrown from file access errors.
     */
    public static com.microsoft.azure.credentials.DelegatedTokenCredentials fromFile(File authFile, String redirectUrl) throws IOException {
        return new com.microsoft.azure.credentials.DelegatedTokenCredentials(ApplicationTokenCredentials.fromFile(authFile), redirectUrl);
    }

    /**
     * Creates a new instance of the DelegatedTokenCredentials from an auth file,
     * with a pre-acquired oauth2 authorization code.
     *
     * @param authFile The credentials based on the file
     * @param redirectUrl the URL to redirect to after authentication in Active Directory
     * @param authorizationCode the oauth2 authorization code
     * @return a new delegated token credentials
     * @throws IOException exception thrown from file access errors.
     */
    public static com.microsoft.azure.credentials.DelegatedTokenCredentials fromFile(File authFile, String redirectUrl, String authorizationCode)
            throws IOException {
        return new com.microsoft.azure.credentials.DelegatedTokenCredentials(ApplicationTokenCredentials.fromFile(authFile), redirectUrl, authorizationCode);
    }

    /**
     * @return the active directory application client id
     */
    public String clientId() {
        return applicationCredentials.clientId();
    }

    /**
     * @return the URL to authenticate through OAuth2
     */
    public String generateAuthenticationUrl(String state) {
        return String.format("%s%s/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s&response_mode=query&state=%s&resource=%s",
                environment().activeDirectoryEndpoint(), domain(), clientId(), redirectUrl, state, environment().managementEndpoint());
    }

    /**
     * Set the authorization code acquired returned to the redirect URL.
     * @param authorizationCode the oauth2 authorization code
     */
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @Override
    public synchronized String getToken(String resource) throws IOException {
        // Find exact match for the resource
        AuthenticationResult authenticationResult = tokens.get(resource);
        // Return if found and not expired
        if (authenticationResult != null && authenticationResult.getExpiresOnDate().after(new Date())) {
            return authenticationResult.getAccessToken();
        }
        // If found then refresh
        boolean shouldRefresh = authenticationResult != null;
        // If not found for the resource, but is MRRT then also refresh
        if (authenticationResult == null && !tokens.isEmpty()) {
            authenticationResult = new ArrayList<>(tokens.values()).get(0);
            shouldRefresh = authenticationResult.isMultipleResourceRefreshToken();
        }
        // Refresh
        if (shouldRefresh) {
            boolean multipleResourceRefreshToken = authenticationResult.isMultipleResourceRefreshToken();
            String refreshToken = authenticationResult.getRefreshToken();
            authenticationResult = acquireAccessTokenFromRefreshToken(resource, refreshToken, multipleResourceRefreshToken);
        }
        // If refresh fails or not refreshable, acquire new token
        if (authenticationResult == null) {
            authenticationResult = acquireNewAccessToken(resource);
        }
        tokens.put(resource, authenticationResult);
        return authenticationResult.getAccessToken();
    }

    public Map<String, AuthenticationResult> getTokens() {
        return new HashMap<>(tokens);
    }

    AuthenticationResult acquireNewAccessToken(String resource) throws IOException {
        if (authorizationCode == null) {
            throw new IllegalArgumentException("You must acquire an authorization code by redirecting to the authentication URL");
        }
        String authorityUrl = environment().activeDirectoryEndpoint() + domain();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AuthenticationContext context = authenticationContextProvider.getAuthenticationContext(authorityUrl, false, executor);
        if (proxy() != null) {
            context.setProxy(proxy());
        }
        try {
            if (clientSecret != null) {
                return context.acquireTokenByAuthorizationCode(
                        authorizationCode,
                        new URI(redirectUrl),
                        new ClientCredential(applicationCredentials.clientId(), clientSecret),
                        resource, null).get();
            }
            throw new AuthenticationException("Please provide either a non-null secret.");
        } catch (URISyntaxException | InterruptedException | ExecutionException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            executor.shutdown();
        }
    }

    private AuthenticationResult acquireAccessTokenFromRefreshToken(String resource, String refreshToken, boolean multipleResourceRefreshToken) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return cbRefreshTokenClient.refreshToken(domain(), clientId(), clientSecret, resource, refreshToken,
                    multipleResourceRefreshToken);
        } catch (Exception e) {
            throw new AuthenticationException("Could not obtain refresh token.", e);
        } finally {
            executor.shutdown();
        }
    }

}
