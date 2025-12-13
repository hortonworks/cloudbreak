package com.sequenceiq.periscope.monitor.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.service.sslcontext.SSLContextProvider;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.config.YarnConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@ExtendWith(MockitoExtension.class)
public class YarnMetricsClientTest {

    private static final String TEST_CLUSTER_CRN = "testCrn";

    private static final String TEST_STACK_NAME = "testStack";

    @Mock
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Mock
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private SSLContextProvider sslContextProvider;

    @Mock
    private Clock clock;

    @Mock
    private YarnConfig yarnConfig;

    @Mock
    private PeriscopeMetricService metricService;

    @Mock
    private RequestLogging requestLogging;

    @Mock
    private UriBuilder uriBuilder;

    @InjectMocks
    private YarnMetricsClient yarnMetricsClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(yarnMetricsClient, "yarnMockClusterUrl", "https://localhost:10090/%s/resourcemanager/v1/cluster/scaling");
    }

    @Test
    void testGetYarnApiUrlForClusterProxy() {
        String cloudProvider = "AWS";
        when(tlsHttpClientConfigurationService.isClusterProxyApplicable(cloudProvider)).thenReturn(true);
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("https://localhost:10080"));
        String yarnEndPoint = yarnMetricsClient.getYarnApiUrl(getAutoScaleCluster(cloudProvider));
        assertEquals("https://localhost:10080/proxy/testCrn/resourcemanager/v1/cluster/scaling", yarnEndPoint);
    }

    @Test
    void testGetYarnApiUrlForClusterProxyWithClusterProxyNotConfigured() {
        String cloudProvider = "AWS";
        when(tlsHttpClientConfigurationService.isClusterProxyApplicable(cloudProvider)).thenReturn(true);
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> yarnMetricsClient.getYarnApiUrl(getAutoScaleCluster(cloudProvider)));
        assertEquals("ClusterProxy Not Configured for Cluster testCrn,  cannot query YARN Metrics.", runtimeException.getMessage());
    }

    @Test
    void testGetYarnApiUrlForMockCloud() {
        String cloudProvider = "MOCK";
        when(tlsHttpClientConfigurationService.isClusterProxyApplicable(cloudProvider)).thenReturn(false);
        String yarnEndPoint = yarnMetricsClient.getYarnApiUrl(getAutoScaleCluster(cloudProvider));
        assertEquals("https://localhost:10090/testCrn/resourcemanager/v1/cluster/scaling", yarnEndPoint);
    }

    @Test
    void testGetYarnApiUrlWithEndpointNotConfigured() {
        String cloudProvider = "AWS";
        when(tlsHttpClientConfigurationService.isClusterProxyApplicable(cloudProvider)).thenReturn(false);
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> yarnMetricsClient.getYarnApiUrl(getAutoScaleCluster(cloudProvider)));
        assertEquals("Endpoint for Yarn Metrics is not configured for testCrn,  cannot query YARN Metrics.", runtimeException.getMessage());
    }

    @Test
    void testGetYarnMetricsForClusterForMock() throws Exception {
        TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
        when(tlsSecurityService.getTls(any())).thenReturn(tlsConfig);
        SSLContext sslContext = mock(SSLContext.class);
        when(sslContextProvider.getSSLContext(tlsConfig.getServerCert(), Optional.empty(), tlsConfig.getClientCert(), tlsConfig.getClientKey()))
                .thenReturn(sslContext);
        try (MockedStatic<UriBuilder> mockedStatic = mockStatic(UriBuilder.class)) {
            UriBuilder uriBuilder = mock(UriBuilder.class);
            mockedStatic.when(() -> UriBuilder.fromPath(anyString())).thenReturn(uriBuilder);
            yarnMetricsClient.getYarnMetricsForCluster(getAutoScaleCluster("MOCK"), null, "pollingUserCrn",
                    Optional.of(2000), Optional.of(2000));
            verify(uriBuilder).queryParam("actionType", "verify");
        }
    }

    @Test
    void testGetYarnMetricsForClusterForNonMock() throws Exception {
        TlsConfiguration tlsConfig = mock(TlsConfiguration.class);
        String cloudProvider = "AWS";
        SSLContext sslContext = mock(SSLContext.class);
        when(sslContextProvider.getSSLContext(tlsConfig.getServerCert(), Optional.empty(), tlsConfig.getClientCert(), tlsConfig.getClientKey()))
                .thenReturn(sslContext);
        when(tlsSecurityService.getTls(any())).thenReturn(tlsConfig);
        when(tlsHttpClientConfigurationService.isClusterProxyApplicable(cloudProvider)).thenReturn(true);
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("https://localhost:10080"));
        try (MockedStatic<UriBuilder> mockedStatic = mockStatic(UriBuilder.class)) {
            UriBuilder uriBuilder = mock(UriBuilder.class);
            mockedStatic.when(() -> UriBuilder.fromPath(anyString())).thenReturn(uriBuilder);
            yarnMetricsClient.getYarnMetricsForCluster(getAutoScaleCluster(cloudProvider), null, "pollingUserCrn",
                    Optional.of(2000), Optional.of(2000));
            verify(uriBuilder, never()).queryParam(anyString(), anyString());
        }
    }

    private Cluster getAutoScaleCluster(String cloudProvider) {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setCloudPlatform(cloudProvider);
        cluster.setStackName(TEST_STACK_NAME);
        return cluster;
    }
}
