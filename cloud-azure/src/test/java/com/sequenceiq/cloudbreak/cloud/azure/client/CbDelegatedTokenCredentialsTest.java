package com.sequenceiq.cloudbreak.cloud.azure.client;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.aad.adal4j.UserInfo;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

public class CbDelegatedTokenCredentialsTest {

    private static final String HTTPS = "https";

    private static final String HTTP = "http";

    private static final String TEST_AD_ENDPOINT = "%s://192.168.0.1";

    private static final String DEFAULT_TEST_AD_ENDPOINT = format(TEST_AD_ENDPOINT, HTTPS);

    private static final String TEST_DOMAIN = "testdomain";

    private static final String REDIRECT_URL = "someotherurl.toredirect";

    private static final String CLIENT_SECRET = "someSecret";

    private static final String RESOURCE = "someResource";

    private static final String ACCESS_TOKEN = "someAccessTokenValue";

    private static final String REFRESH_TOKEN = "someRefreshToken";

    private static final String AUTHORIZATION_CODE = "someAuthCode";

    private static final String CLIENT_ID = "1234";

    private static final boolean MULTIPLE_RESOURCE_REFRESH_TOKEN = Boolean.TRUE;

    private static final String MANAGEMENT_ENDPOINT_URL = "https://managementendpoint.com";

    private static final long DIFFERENCE = 10;

    private static final long FUTURE_DATE = now().plus(DIFFERENCE, DAYS).toEpochMilli();

    private static final long PAST_DATE = 0L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ApplicationTokenCredentials applicationTokenCredentials;

    @Mock
    private AuthenticationContextProvider authenticationContextProvider;

    @Mock
    private AuthenticationContext authenticationContext;

    @Mock
    private Future<AuthenticationResult> futureAuthenticationResult;

    @Mock
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Mock
    private UserInfo userInfo;

    @Mock
    private CBRefreshTokenClient cbRefreshTokenClient;

    private Map<String, AuthenticationResult> tokens;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokens = new LinkedHashMap<>();
        tokens.put(RESOURCE, new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, FUTURE_DATE, "1", userInfo,
                MULTIPLE_RESOURCE_REFRESH_TOKEN));
        AzureEnvironment defaultAzureEnvironment = new AzureEnvironment(Map.of("activeDirectoryEndpointUrl", DEFAULT_TEST_AD_ENDPOINT,
                "managementEndpointUrl", MANAGEMENT_ENDPOINT_URL));
        when(applicationTokenCredentials.environment()).thenReturn(defaultAzureEnvironment);
        when(applicationTokenCredentials.domain()).thenReturn(TEST_DOMAIN);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);
    }

    @Test
    public void testAcquireNewAccessTokenWhenNoAuthorizationCodeThenExceptionComes() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("You must acquire an authorization code by redirecting to the authentication URL");

        new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider).acquireNewAccessToken(RESOURCE);

        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testGetTokenWhenAccessTokenExistsThenItComesBack() throws IOException {
        String result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider).getToken(RESOURCE);

        assertEquals(ACCESS_TOKEN, result);

        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testGetTokenWhenNoTokenAndAuthCodeProvidedThenIllegalArgumentExceptionComes() throws IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("You must acquire an authorization code by redirecting to the authentication URL");

        new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, Collections.emptyMap(), CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider).getToken(RESOURCE);

        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    public void testGetTokenWhenAuthCodeGivenButNoTokenProvidedAndHttpUsedAsActiveDirectoryEndpointProtocolInsteadOfHttpsThenExceptionComes()
            throws IOException {
        String authorityUrl = format("%s/%s", format(TEST_AD_ENDPOINT, HTTP), TEST_DOMAIN);
        when(applicationTokenCredentials.environment()).thenReturn(new AzureEnvironment(Map.of("activeDirectoryEndpointUrl", format(TEST_AD_ENDPOINT, HTTP))));
        doThrow(new IllegalArgumentException("'authority' should use the 'https' scheme")).when(authenticationContextProvider)
                .getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));

        var underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, authenticationContextProvider, cbRefreshTokenClientProvider);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("'authority' should use the 'https' scheme");

        underTest.getToken(RESOURCE);

        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));
    }

    @Test
    public void testGetTokenWhenNoSecretProvidedThenAuthenticationExceptionComes() throws IOException {
        String authorityUrl = format("%s/%s", format(TEST_AD_ENDPOINT, HTTP), TEST_DOMAIN);
        var underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, authenticationContextProvider, cbRefreshTokenClientProvider);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expect(AuthenticationException.class);
        thrown.expectMessage("Please provide either a non-null secret.");

        underTest.getToken(RESOURCE);

        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));
    }

    @Test
    public void testGetTokenClientSecretAndAuthorizationCodeGivenThroughConstructorThenNewAccessTokenReturns() throws IOException, ExecutionException,
            InterruptedException {
        String customAccessToken = "customAccessToken";
        String authorityUrl = format("%s/%s", format(TEST_AD_ENDPOINT, HTTPS), TEST_DOMAIN);
        AuthenticationResult authenticationResult = new AuthenticationResult("type", customAccessToken, REFRESH_TOKEN, 123456789L, "1", mock(UserInfo.class),
                true);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);
        when(authenticationContextProvider.getAuthenticationContext(eq(authorityUrl), eq(false),
                any(ExecutorService.class))).thenReturn(authenticationContext);
        when(authenticationContext.acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class), eq(RESOURCE), eq(null)))
                .thenReturn(futureAuthenticationResult);
        when(futureAuthenticationResult.get()).thenReturn(authenticationResult);

        String result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, AUTHORIZATION_CODE, CLIENT_SECRET,
                authenticationContextProvider, cbRefreshTokenClientProvider).getToken(RESOURCE);

        Assert.assertNotEquals(ACCESS_TOKEN, result);
        assertEquals(customAccessToken, result);

        verify(futureAuthenticationResult, times(1)).get();
        verify(applicationTokenCredentials, times(1)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class),
                eq(RESOURCE), eq(null));
    }

    @Test
    public void testAcquireNewAccessTokenWhenAuthenticationResultGetFailsDueToExecutionExceptionThenIOExceptionComes() throws IOException, ExecutionException,
            InterruptedException {
        String authorityUrl = format("%s/%s", format(TEST_AD_ENDPOINT, HTTPS), TEST_DOMAIN);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);
        when(authenticationContextProvider.getAuthenticationContext(eq(authorityUrl), eq(false),
                any(ExecutorService.class))).thenReturn(authenticationContext);
        when(authenticationContext.acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class), eq(RESOURCE), eq(null)))
                .thenReturn(futureAuthenticationResult);
        doThrow(new ExecutionException("some execution failure", new RuntimeException())).when(futureAuthenticationResult).get();

        thrown.expect(IOException.class);
        thrown.expectMessage("some execution failure");


        new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, AUTHORIZATION_CODE, CLIENT_SECRET,
                authenticationContextProvider, cbRefreshTokenClientProvider).acquireNewAccessToken(RESOURCE);

        verify(futureAuthenticationResult, times(1)).get();
        verify(applicationTokenCredentials, times(1)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class),
                eq(RESOURCE), eq(null));
    }

    @Test
    public void testAcquireNewAccessTokenWhenAuthenticationResultGetFailsDueToInterruptedExceptionThenIOExceptionComes() throws IOException, ExecutionException,
            InterruptedException {
        String authorityUrl = format("%s/%s", format(TEST_AD_ENDPOINT, HTTPS), TEST_DOMAIN);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);
        when(authenticationContextProvider.getAuthenticationContext(eq(authorityUrl), eq(false),
                any(ExecutorService.class))).thenReturn(authenticationContext);
        when(authenticationContext.acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class), eq(RESOURCE), eq(null)))
                .thenReturn(futureAuthenticationResult);
        doThrow(new InterruptedException("some interrupted me!")).when(futureAuthenticationResult).get();

        thrown.expect(IOException.class);
        thrown.expectMessage("some interrupted me!");


        new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, AUTHORIZATION_CODE, CLIENT_SECRET,
                authenticationContextProvider, cbRefreshTokenClientProvider).acquireNewAccessToken(RESOURCE);

        verify(futureAuthenticationResult, times(1)).get();
        verify(applicationTokenCredentials, times(1)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContextProvider, times(1)).getAuthenticationContext(eq(authorityUrl), eq(false), any(ExecutorService.class));
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
        verify(authenticationContext, times(1)).acquireTokenByAuthorizationCode(eq(AUTHORIZATION_CODE), any(URI.class), any(ClientCredential.class),
                eq(RESOURCE), eq(null));
    }

    @Test
    public void testGetTokensWhenWeSetTokensThroughConstructorThenItShouldBeComeBackUntouched() {
        Map<String, AuthenticationResult> result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET,
                authenticationContextProvider, cbRefreshTokenClientProvider).getTokens();

        assertNotNull(result);
        assertEquals(tokens.size(), result.size());
        result.forEach((key, authenticationResult1) -> {
            assertTrue(tokens.containsKey(key));
            assertEquals(tokens.get(key), authenticationResult1);
        });

        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
    }

    @Test
    public void testGenerateAuthenticationUrlWhenCallingThePatternIsTheExpected() {
        String state = "someState";
        String expected = String.format("%s%s/oauth2/authorize?client_id=%s&response_type=code&redirect_uri=%s&response_mode=query&state=%s&resource=%s",
                String.format("%s/", DEFAULT_TEST_AD_ENDPOINT), TEST_DOMAIN, CLIENT_ID, REDIRECT_URL, state, MANAGEMENT_ENDPOINT_URL);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);

        String result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, AUTHORIZATION_CODE, CLIENT_SECRET,
                authenticationContextProvider, cbRefreshTokenClientProvider).generateAuthenticationUrl(state);

        assertEquals(expected, result);

        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
    }

    @Test
    public void testGetTokenWhenAuthenticationResultNotFoundForTheResourceButIsMRRTAndMultipleResourceRefreshTokenIsFalseThenGivenTokenShouldReturn()
            throws IOException, ExecutionException, InterruptedException {
        String customResource = "someOtherResourceWhichIsNotInTheTokensMap";

        Map<String, AuthenticationResult> tokens = Map.of(RESOURCE, new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, PAST_DATE, "1",
                mock(UserInfo.class), false));

        String result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider).getToken(customResource);

        assertEquals(ACCESS_TOKEN, result);

        verify(futureAuthenticationResult, times(0)).get();
        verify(applicationTokenCredentials, times(0)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContext, times(0)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
    }

    @Test
    public void testGetTokenWhenDifferentResourceGivenThanProvidedInTokensAndShouldRefreshThenNewAccessTokenReturnsAfterwards()
            throws IOException, ExecutionException, InterruptedException {
        String expected = "someOtherAccessToken";
        String customResource = "someOtherResourceWhichIsNotInTheTokensMap";

        Map<String, AuthenticationResult> tokens = Map.of(RESOURCE, new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, PAST_DATE,
                "1", mock(UserInfo.class),
                true));

        AuthenticationResult refreshTokenFromAccessTokenResult = new AuthenticationResult("type", expected, REFRESH_TOKEN,
                PAST_DATE, "2", userInfo, true);

        when(cbRefreshTokenClientProvider.getCBRefreshTokenClient(eq(String.format("%s/", DEFAULT_TEST_AD_ENDPOINT)))).thenReturn(cbRefreshTokenClient);
        when(cbRefreshTokenClient.refreshToken(TEST_DOMAIN, CLIENT_ID, CLIENT_SECRET, customResource, REFRESH_TOKEN, MULTIPLE_RESOURCE_REFRESH_TOKEN))
                .thenReturn(refreshTokenFromAccessTokenResult);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);

        String result = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider)
                .getToken(customResource);

        assertEquals(expected, result);

        verify(futureAuthenticationResult, times(0)).get();
        verify(applicationTokenCredentials, times(1)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(1)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContext, times(0)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
        verify(cbRefreshTokenClient, times(1)).refreshToken(TEST_DOMAIN, CLIENT_ID, CLIENT_SECRET, customResource, REFRESH_TOKEN,
                MULTIPLE_RESOURCE_REFRESH_TOKEN);
    }

    @Test
    public void testGetTokenWhenDifferentResourceGivenThanProvidedInTokensAndShouldRefreshAndRefreshingTokenFailsThenAuthenticationExceptionComes()
            throws IOException, ExecutionException, InterruptedException {
        String customResource = "someOtherResourceWhichIsNotInTheTokensMap";

        Map<String, AuthenticationResult> tokens = Map.of(RESOURCE, new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, PAST_DATE,
                "1", mock(UserInfo.class),
                true));

        when(cbRefreshTokenClientProvider.getCBRefreshTokenClient(eq(String.format("%s/", DEFAULT_TEST_AD_ENDPOINT)))).thenReturn(cbRefreshTokenClient);
        doThrow(new RuntimeException()).when(cbRefreshTokenClient).refreshToken(TEST_DOMAIN, CLIENT_ID, CLIENT_SECRET, customResource, REFRESH_TOKEN,
                MULTIPLE_RESOURCE_REFRESH_TOKEN);
        when(applicationTokenCredentials.clientId()).thenReturn(CLIENT_ID);

        thrown.expect(AuthenticationException.class);
        thrown.expectMessage("Could not obtain refresh token.");

        new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET, authenticationContextProvider,
                cbRefreshTokenClientProvider)
                .getToken(customResource);

        verify(futureAuthenticationResult, times(0)).get();
        verify(applicationTokenCredentials, times(1)).clientId();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClient, times(1)).refreshToken(TEST_DOMAIN, CLIENT_ID, CLIENT_SECRET, customResource, REFRESH_TOKEN,
                MULTIPLE_RESOURCE_REFRESH_TOKEN);
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(format("%s/", DEFAULT_TEST_AD_ENDPOINT)));
        verify(authenticationContextProvider, times(0)).getAuthenticationContext(anyString(), anyBoolean(), any(ExecutorService.class));
        verify(cbRefreshTokenClient, times(1)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(authenticationContext, times(0)).acquireTokenByAuthorizationCode(anyString(), any(URI.class), any(ClientCredential.class), anyString(), any());
    }

}