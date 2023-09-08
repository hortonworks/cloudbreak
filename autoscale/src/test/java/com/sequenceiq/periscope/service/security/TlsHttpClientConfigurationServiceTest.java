package com.sequenceiq.periscope.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@ExtendWith(MockitoExtension.class)
public class TlsHttpClientConfigurationServiceTest {

    private static final String TEST_CLUSTER_CRN = "testCrn";

    private static final String TEST_STACK_NAME = "testStack";

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @InjectMocks
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tlsHttpClientConfigurationService, "clusterProxyDisabledPlatforms", Set.of("MOCK"));
        when(tlsSecurityService.getTls(any())).thenReturn(new TlsConfiguration("key", "cert", "serverCert"));
    }

    @Test
    void testBuildTLSClientConfigForAws() {
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("http://localost:10010"));
        HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(getAutoScaleCluster("AWS"));
        assertEquals("http://localost:10010", httpClientConfig.getClusterProxyUrl());
        assertEquals(TEST_CLUSTER_CRN, httpClientConfig.getClusterCrn());
    }

    @Test
    void testBuildTLSClientConfigForMock() {
        HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(getAutoScaleCluster("MOCK"));
        assertNull(httpClientConfig.getClusterProxyUrl());
        assertNull(httpClientConfig.getClusterCrn());
    }

    private Cluster getAutoScaleCluster(String cloudProvider) {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setCloudPlatform(cloudProvider);
        cluster.setStackName(TEST_STACK_NAME);
        cluster.setClusterManager(new ClusterManager());
        return cluster;
    }

}
