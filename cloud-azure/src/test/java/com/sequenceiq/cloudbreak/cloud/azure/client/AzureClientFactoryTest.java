package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.client.traits.HttpTrait;
import com.azure.core.http.HttpClient;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.marketplaceordering.MarketplaceOrderingManager;
import com.azure.resourcemanager.privatedns.PrivateDnsZoneManager;
import com.azure.resourcemanager.resources.fluentcore.arm.AzureConfigurable;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class AzureClientFactoryTest {

    private static final String TENANT_ID = "1";

    private static final String ACCESS_KEY = "123";

    private static final String SECRET_KEY = "someSecretKey";

    private static final String SUBSCRIPTION_ID = "4321";

    private static final Region REGION = Region.region("westus2");

    private static final String CREDENTIAL_NAME = "someCredName";

    private static final HttpLogDetailLevel LOG_LEVEL = HttpLogDetailLevel.BASIC;

    @Mock
    private AzureCredentialView credentialView;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private ExecutorService mdcCopyingThreadPoolExecutor;

    @Mock
    private AzureHttpClientConfigurer azureHttpClientConfigurer;

    @Mock
    private HttpClient httpClient;

    @BeforeEach
    public void setUp() {
        when(credentialView.getTenantId()).thenReturn(TENANT_ID);
        when(credentialView.getAccessKey()).thenReturn(ACCESS_KEY);
        when(credentialView.getSecretKey()).thenReturn(SECRET_KEY);
        when(credentialView.getName()).thenReturn(CREDENTIAL_NAME);
        when(credentialView.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID);
        when(azureHttpClientConfigurer.newHttpClient()).thenReturn(httpClient);
        lenient().doAnswer(invocation -> invocation.getArguments()[0]).when(azureHttpClientConfigurer).configureDefault(any(HttpTrait.class));
        lenient().doAnswer(invocation -> invocation.getArguments()[0]).when(azureHttpClientConfigurer).configureDefault(any(AzureConfigurable.class));
        lenient().doAnswer(invocation -> invocation.getArguments()[0])
                .when(azureHttpClientConfigurer).configureDefault(any(MarketplaceOrderingManager.Configurable.class));
    }

    @Test
    public void testGetAzureAgainstSubscriptionId() {
        AzureResourceManager result = new AzureClientFactory(null, credentialView, mdcCopyingThreadPoolExecutor, azureHttpClientConfigurer)
                .getAzureResourceManager();

        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());

        verify(credentialView, times(1)).getAuthenticationType();
        verify(credentialView, times(0)).getCertificate();
        verify(credentialView, times(0)).getPrivateKeyForCertificate();
        verify(credentialView, times(2)).getTenantId();
        verify(credentialView, times(1)).getAccessKey();
        verify(credentialView, times(1)).getSecretKey();
        verify(credentialView, times(1)).getSubscriptionId();
    }

    @Test
    public void testGetPrivateDnsManagerForSubscriptionId() {
        PrivateDnsZoneManager result =
                new AzureClientFactory(null, credentialView, mdcCopyingThreadPoolExecutor, azureHttpClientConfigurer).getPrivateDnsManager();
        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());
    }

    @Test
    public void testGetComputeManagerForSubscriptionId() {
        ComputeManager result = new AzureClientFactory(null, credentialView, mdcCopyingThreadPoolExecutor, azureHttpClientConfigurer).getComputeManager();
        assertNotNull(result);
        assertEquals(SUBSCRIPTION_ID, result.subscriptionId());
    }

    @Test
    public void testRegionAwareEndpointIsSet() throws MalformedURLException {
        String originalResourceManagerEndpoint = AzureEnvironment.AZURE.getResourceManagerEndpoint();
        when(cloudContext.getLocation()).thenReturn(Location.location(REGION));
        AzureEnvironment azureEnvironment = new AzureClientFactory(cloudContext, credentialView, mdcCopyingThreadPoolExecutor, azureHttpClientConfigurer)
                .getAzureEnvironment();

        URL resourceManagerEndpointUrl = new URL(originalResourceManagerEndpoint);
        String regionAwareUrl = String.format("%s://%s.%s",
                resourceManagerEndpointUrl.getProtocol(),
                REGION.getRegionName(),
                resourceManagerEndpointUrl.getHost());
        assertNotNull(azureEnvironment);
        assertNotEquals(originalResourceManagerEndpoint, azureEnvironment.getResourceManagerEndpoint());
        assertEquals(regionAwareUrl, azureEnvironment.getResourceManagerEndpoint());
    }

}