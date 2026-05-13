package com.sequenceiq.cloudbreak.cloud.openstack.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigService;

@ExtendWith(MockitoExtension.class)
class OpenStackClusterProxyServiceTest {

    private static final String ACCOUNT_ID = "hortonworks";

    private static final String CREDENTIAL_NAME = "my-openstack";

    private static final String EXPECTED_CRN = "crn:cdp:openstack-jumpgate:us-west-1:hortonworks:jumpgate:my-openstack";

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @InjectMocks
    private OpenStackClusterProxyService underTest;

    @BeforeEach
    void setUp() {
        setRegion("us-west-1");
    }

    @Test
    void generateClusterCrnShouldReturnCorrectFormat() {
        String crn = underTest.generateClusterCrn(ACCOUNT_ID, CREDENTIAL_NAME);

        assertEquals(EXPECTED_CRN, crn);
    }

    @Test
    void generateClusterCrnShouldUseConfiguredRegion() {
        setRegion("eu-1");

        String crn = underTest.generateClusterCrn(ACCOUNT_ID, CREDENTIAL_NAME);

        assertEquals("crn:cdp:openstack-jumpgate:eu-1:hortonworks:jumpgate:my-openstack", crn);
    }

    @Test
    void buildProxyBaseUrlShouldCombineConfigAndCrn() {
        when(clusterProxyConfiguration.getClusterProxyUrl()).thenReturn("http://localhost:10180/cluster-proxy");
        when(clusterProxyConfiguration.getHttpProxyPath()).thenReturn("/proxy");

        String result = underTest.buildProxyBaseUrl(EXPECTED_CRN);

        assertEquals("http://localhost:10180/cluster-proxy/proxy/" + EXPECTED_CRN, result);
    }

    @Test
    void buildProxyUrlShouldAppendServiceName() {
        when(clusterProxyConfiguration.getClusterProxyUrl()).thenReturn("http://localhost:10180/cluster-proxy");
        when(clusterProxyConfiguration.getHttpProxyPath()).thenReturn("/proxy");

        String result = underTest.buildProxyUrl(EXPECTED_CRN, "nova");

        assertEquals("http://localhost:10180/cluster-proxy/proxy/" + EXPECTED_CRN + "/nova", result);
    }

    @Test
    void isRegisteredShouldReturnTrueWhenServicesExist() {
        ReadConfigResponse response = new ReadConfigResponse();
        ReadConfigService service = new ReadConfigService();
        response.setServices(List.of(service));
        when(clusterProxyRegistrationClient.readConfig(EXPECTED_CRN)).thenReturn(response);

        boolean result = underTest.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME);

        assertTrue(result);
    }

    @Test
    void isRegisteredShouldReturnFalseWhenServicesEmpty() {
        ReadConfigResponse response = new ReadConfigResponse();
        response.setServices(List.of());
        when(clusterProxyRegistrationClient.readConfig(EXPECTED_CRN)).thenReturn(response);

        boolean result = underTest.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME);

        assertFalse(result);
    }

    @Test
    void isRegisteredShouldReturnFalseWhenServicesNull() {
        ReadConfigResponse response = new ReadConfigResponse();
        response.setServices(null);
        when(clusterProxyRegistrationClient.readConfig(EXPECTED_CRN)).thenReturn(response);

        boolean result = underTest.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME);

        assertFalse(result);
    }

    @Test
    void isRegisteredShouldReturnFalseWhenResponseNull() {
        when(clusterProxyRegistrationClient.readConfig(EXPECTED_CRN)).thenReturn(null);

        boolean result = underTest.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME);

        assertFalse(result);
    }

    @Test
    void isRegisteredShouldReturnFalseWhenClusterProxyThrows() {
        when(clusterProxyRegistrationClient.readConfig(EXPECTED_CRN))
                .thenThrow(new ClusterProxyException("not found", new RuntimeException()));

        boolean result = underTest.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME);

        assertFalse(result);
    }

    @Test
    void deregisterServicesShouldCallClient() {
        underTest.deregisterServices(ACCOUNT_ID, CREDENTIAL_NAME);

        verify(clusterProxyRegistrationClient).deregisterConfig(EXPECTED_CRN);
    }

    private void setRegion(String value) {
        try {
            Field field = OpenStackClusterProxyService.class.getDeclaredField("region");
            field.setAccessible(true);
            field.set(underTest, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set region field for test", e);
        }
    }
}
