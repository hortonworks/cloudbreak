package com.sequenceiq.cloudbreak.cloud.openstack.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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

    private static final String CREDENTIAL_CRN = "crn:cdp:credential:us-west-1:hortonworks:credential:my-openstack-uuid";

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @InjectMocks
    private OpenStackClusterProxyService underTest;

    @Test
    void buildProxyBaseUrlShouldCombineConfigAndCrn() {
        when(clusterProxyConfiguration.getClusterProxyUrl()).thenReturn("http://localhost:10180/cluster-proxy");
        when(clusterProxyConfiguration.getHttpProxyPath()).thenReturn("/proxy");

        String result = underTest.buildProxyBaseUrl(CREDENTIAL_CRN);

        assertEquals("http://localhost:10180/cluster-proxy/proxy/" + CREDENTIAL_CRN, result);
    }

    @Test
    void buildProxyUrlShouldAppendServiceName() {
        when(clusterProxyConfiguration.getClusterProxyUrl()).thenReturn("http://localhost:10180/cluster-proxy");
        when(clusterProxyConfiguration.getHttpProxyPath()).thenReturn("/proxy");

        String result = underTest.buildProxyUrl(CREDENTIAL_CRN, "nova");

        assertEquals("http://localhost:10180/cluster-proxy/proxy/" + CREDENTIAL_CRN + "/nova", result);
    }

    @Test
    void isRegisteredShouldReturnTrueWhenServicesExist() {
        ReadConfigResponse response = new ReadConfigResponse();
        ReadConfigService service = new ReadConfigService();
        response.setServices(List.of(service));
        when(clusterProxyRegistrationClient.readConfig(CREDENTIAL_CRN)).thenReturn(response);

        assertTrue(underTest.isRegistered(CREDENTIAL_CRN));
    }

    @Test
    void isRegisteredShouldReturnFalseWhenServicesEmpty() {
        ReadConfigResponse response = new ReadConfigResponse();
        response.setServices(List.of());
        when(clusterProxyRegistrationClient.readConfig(CREDENTIAL_CRN)).thenReturn(response);

        assertFalse(underTest.isRegistered(CREDENTIAL_CRN));
    }

    @Test
    void isRegisteredShouldReturnFalseWhenServicesNull() {
        ReadConfigResponse response = new ReadConfigResponse();
        response.setServices(null);
        when(clusterProxyRegistrationClient.readConfig(CREDENTIAL_CRN)).thenReturn(response);

        assertFalse(underTest.isRegistered(CREDENTIAL_CRN));
    }

    @Test
    void isRegisteredShouldReturnFalseWhenResponseNull() {
        when(clusterProxyRegistrationClient.readConfig(CREDENTIAL_CRN)).thenReturn(null);

        assertFalse(underTest.isRegistered(CREDENTIAL_CRN));
    }

    @Test
    void isRegisteredShouldReturnFalseWhenClusterProxyThrows() {
        when(clusterProxyRegistrationClient.readConfig(CREDENTIAL_CRN))
                .thenThrow(new ClusterProxyException("not found", new RuntimeException()));

        assertFalse(underTest.isRegistered(CREDENTIAL_CRN));
    }

    @Test
    void deregisterServicesShouldCallClient() {
        underTest.deregisterServices(CREDENTIAL_CRN);

        verify(clusterProxyRegistrationClient).deregisterConfig(CREDENTIAL_CRN);
    }
}
