package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
@Disabled
class AzureClientServiceTest {

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @InjectMocks
    private AzureClientService underTest;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "logLevel", HttpLogDetailLevel.BASIC);
        Map<String, Object> parameters = Map.of(
                "tenantId", "tenant1",
                "appBased", Map.of("accessKey", "accessKey1", "secretKey", "secretKey1"));
        when(cloudCredential.getParameters()).thenReturn(Map.of("azure", parameters));
        when(cloudCredential.getParameter(eq("azure"), any())).thenReturn(parameters);
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

    @Test
    void getClientWithContextTest() {
        AzureClient azureClient = underTest.getClient(cloudContext, cloudCredential);
        verifyAzureClient(azureClient);
    }

    private void verifyAzureClient(AzureClient azureClient) {
        assertThat(azureClient).isNotNull();

        AzureClientFactory azureClientCredentials = (AzureClientFactory) ReflectionTestUtils.getField(azureClient, "azureClientCredentials");
        assertThat(azureClientCredentials).isNotNull();

        AzureCredentialView azureCredentialView = (AzureCredentialView) ReflectionTestUtils.getField(azureClientCredentials, "credentialView");
        assertThat(azureCredentialView).isNotNull();

        assertThat((CloudCredential) ReflectionTestUtils.getField(azureCredentialView, "cloudCredential")).isSameAs(cloudCredential);
    }

}