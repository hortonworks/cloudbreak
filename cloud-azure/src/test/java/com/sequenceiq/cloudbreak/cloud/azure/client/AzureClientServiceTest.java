package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.tracing.AzureOkHttp3TracingInterceptor;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
class AzureClientServiceTest {

    @Mock
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Mock
    private AuthenticationContextProvider authenticationContextProvider;

    @Mock
    private AzureOkHttp3TracingInterceptor tracingInterceptor;

    @Mock
    private AzureAuthExceptionHandler azureAuthExceptionHandler;

    @InjectMocks
    private AzureClientService underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "logLevel", LogLevel.BASIC);
    }

    @Test
    void createAuthenticatedContextTest() {
        AuthenticatedContext authenticatedContext = underTest.createAuthenticatedContext(cloudContext, cloudCredential);

        assertThat(authenticatedContext).isNotNull();
        assertThat(authenticatedContext.getCloudContext()).isSameAs(cloudContext);
        assertThat(authenticatedContext.getCloudCredential()).isSameAs(cloudCredential);
        verifyAzureClient(authenticatedContext.getParameter(AzureClient.class));
    }

    @Test
    void getClientTest() {
        AzureClient azureClient = underTest.getClient(cloudCredential);

        verifyAzureClient(azureClient);
    }

    private void verifyAzureClient(AzureClient azureClient) {
        assertThat(azureClient).isNotNull();

        AzureClientCredentials azureClientCredentials = (AzureClientCredentials) ReflectionTestUtils.getField(azureClient, "azureClientCredentials");
        assertThat(azureClientCredentials).isNotNull();

        AzureCredentialView azureCredentialView = (AzureCredentialView) ReflectionTestUtils.getField(azureClientCredentials, "credentialView");
        assertThat(azureCredentialView).isNotNull();

        assertThat((CloudCredential) ReflectionTestUtils.getField(azureCredentialView, "cloudCredential")).isSameAs(cloudCredential);
    }

}