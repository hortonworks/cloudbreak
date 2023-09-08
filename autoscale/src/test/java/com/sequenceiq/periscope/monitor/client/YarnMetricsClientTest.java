package com.sequenceiq.periscope.monitor.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;

@ExtendWith(MockitoExtension.class)
public class YarnMetricsClientTest {

    private static final String TEST_CLUSTER_CRN = "testCrn";

    private static final String TEST_STACK_NAME = "testStack";

    @Mock
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Mock
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

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

    private Cluster getAutoScaleCluster(String cloudProvider) {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setCloudPlatform(cloudProvider);
        cluster.setStackName(TEST_STACK_NAME);
        return cluster;
    }
}
