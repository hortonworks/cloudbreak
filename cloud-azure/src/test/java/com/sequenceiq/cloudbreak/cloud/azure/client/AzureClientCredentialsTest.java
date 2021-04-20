package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.UserInfo;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.implementation.ComputeManager;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.privatednsManager;
import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.tracing.AzureOkHttp3TracingInterceptor;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AzureClientCredentialsTest {

    private static final String TENANT_ID = "1";

    private static final String ACCESS_KEY = "123";

    private static final String SECRET_KEY = "someSecretKey";

    private static final String SUBSCRIPTION_ID = "4321";

    private static final String CREDENTIAL_NAME = "someCredName";

    private static final String ACCESS_TOKEN = "someAccessTokenValue";

    private static final String REFRESH_TOKEN = "someRefreshToken";

    private static final LogLevel LOG_LEVEL = LogLevel.BASIC;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private AzureCredentialView credentialView;

    @Mock
    private CBRefreshTokenClient cbRefreshTokenClient;

    @Mock
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Mock
    private AuthenticationContextProvider authenticationContextProvider;

    @Mock
    private AzureOkHttp3TracingInterceptor tracingInterceptor;

    private AuthenticationResult authenticationResult;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(credentialView.codeGrantFlow()).thenReturn(true);
        when(credentialView.getTenantId()).thenReturn(TENANT_ID);
        when(credentialView.getAccessKey()).thenReturn(ACCESS_KEY);
        when(credentialView.getSecretKey()).thenReturn(SECRET_KEY);
        when(credentialView.getName()).thenReturn(CREDENTIAL_NAME);
        when(credentialView.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(cbRefreshTokenClientProvider.getCBRefreshTokenClient(eq(AzureEnvironment.AZURE.activeDirectoryEndpoint()))).thenReturn(cbRefreshTokenClient);
        authenticationResult = new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, 123456789L, "1", mock(UserInfo.class), true);
    }

    @Test
    public void testGetRefreshTokenWhenCredentialFlowIsNotCodeGrantFlowThenEmptyOptionalReturns() {
        when(credentialView.codeGrantFlow()).thenReturn(false);

        Optional<String> result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor)
                .getRefreshToken();

        assertFalse(result.isPresent());

        verify(credentialView, times(1)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(2)).codeGrantFlow();
        verify(credentialView, times(1)).getSubscriptionId();
        verify(cbRefreshTokenClientProvider, times(0)).getCBRefreshTokenClient(anyString());
    }

    @Test
    public void testGetRefreshTokenWhenBothRefreshTokenAndAuthenticationResultIsObtainableThenExpectedRefreshTokenReturns() {
        when(credentialView.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(cbRefreshTokenClient.refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(authenticationResult);

        Optional<String> result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor)
                .getRefreshToken();

        assertTrue(result.isPresent());
        assertEquals(REFRESH_TOKEN, result.get());

        verify(credentialView, times(1)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(2)).codeGrantFlow();
        verify(credentialView, times(1)).getSubscriptionId();
        verify(cbRefreshTokenClientProvider, times(2)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClient, times(1)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(cbRefreshTokenClientProvider, times(2)).getCBRefreshTokenClient(eq(AzureEnvironment.AZURE.activeDirectoryEndpoint()));
    }

    @Test
    public void testGetRefreshTokenWhenThereIsNoRefreshTokenInCredentialViewThenNoRefreshTokenReturns() {
        when(credentialView.getRefreshToken()).thenReturn(null);
        when(credentialView.getAppReplyUrl()).thenReturn("replyUrl");
        when(credentialView.getAuthorizationCode()).thenReturn("someAuthCode");

        Optional<String> result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor)
                .getRefreshToken();

        assertFalse(result.isPresent());

        verify(credentialView, times(1)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(1)).getAppReplyUrl();
        verify(credentialView, times(1)).getRefreshToken();
        verify(credentialView, times(2)).codeGrantFlow();
        verify(credentialView, times(1)).getSubscriptionId();
        verify(credentialView, times(1)).getAuthorizationCode();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(AzureEnvironment.AZURE.activeDirectoryEndpoint()));
    }

    @Test
    public void testInstanceCreationWhenUnableToRefreshTokenThenCloudConnectorExceptionComes() {
        when(credentialView.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(cbRefreshTokenClient.refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(null);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(String.format("New token couldn't be obtain with refresh token for credential: %s", CREDENTIAL_NAME));

        new AzureClientCredentials(credentialView, LOG_LEVEL, cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor);

        verify(credentialView, times(1)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(1)).getAppReplyUrl();
        verify(credentialView, times(1)).getRefreshToken();
        verify(credentialView, times(2)).codeGrantFlow();
        verify(credentialView, times(1)).getSubscriptionId();
        verify(credentialView, times(1)).getAuthorizationCode();
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClient, times(0)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(cbRefreshTokenClientProvider, times(1)).getCBRefreshTokenClient(eq(AzureEnvironment.AZURE.activeDirectoryEndpoint()));
    }

    @Test
    public void testGetAzureAgainstSubscriptionId() {
        when(credentialView.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(cbRefreshTokenClient.refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(authenticationResult);

        Azure result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor).getAzure();

        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());

        verify(credentialView, times(1)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(0)).getAppReplyUrl();
        verify(credentialView, times(1)).getRefreshToken();
        verify(credentialView, times(1)).codeGrantFlow();
        verify(credentialView, times(2)).getSubscriptionId();
        verify(credentialView, times(0)).getAuthorizationCode();
        verify(cbRefreshTokenClientProvider, times(2)).getCBRefreshTokenClient(anyString());
        verify(cbRefreshTokenClient, times(1)).refreshToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(cbRefreshTokenClientProvider, times(2)).getCBRefreshTokenClient(eq(AzureEnvironment.AZURE.activeDirectoryEndpoint()));
    }

    @Test
    public void testGetPrivateDnsManagerForSubscriptionId() {
        privatednsManager result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor).getPrivateDnsManager();
        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());
    }

    @Test
    public void testGetComputeManagerForSubscriptionId() {
        ComputeManager result = new AzureClientCredentials(credentialView, LOG_LEVEL,
                cbRefreshTokenClientProvider, authenticationContextProvider, tracingInterceptor).getComputeManager();
        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());
    }

}