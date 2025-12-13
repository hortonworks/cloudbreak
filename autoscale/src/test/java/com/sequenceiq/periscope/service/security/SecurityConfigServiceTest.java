package com.sequenceiq.periscope.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;

@ExtendWith(MockitoExtension.class)
class SecurityConfigServiceTest {

    private static final String TEST_CLUSTER_CRN = "testCrn";

    private static final String TEST_STACK_NAME = "testStack";

    @Mock
    private SecurityConfigRepository securityConfigRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @InjectMocks
    private SecurityConfigService underTest;

    @Test
    void testWhenSecurityConfigAvailable() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setServerCert("serverCert");
        securityConfig.setClientKey("clientKey");
        securityConfig.setClientCert("clientCert");
        Cluster cluster = getAutoScaleCluster();
        securityConfig.setCluster(cluster);

        when(securityConfigRepository.findByClusterId(anyLong())).thenReturn(securityConfig);
        SecurityConfig result = underTest.getSecurityConfig(34L);
        assertEquals(securityConfig.getClientCert(), result.getClientCert());
    }

    @Test
    void testWhenSecurityConfigObjectisNull() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setServerCert("serverCert");
        securityConfig.setClientKey("clientKey");
        securityConfig.setClientCert("clientCert");
        Cluster cluster = getAutoScaleCluster();
        securityConfig.setCluster(cluster);

        when(securityConfigRepository.findByClusterId(anyLong())).thenReturn(null);
        when(clusterRepository.findStackCrnById(anyLong())).thenReturn(TEST_CLUSTER_CRN);
        when(cloudbreakCommunicator.getRemoteSecurityConfig(anyString())).thenReturn(securityConfig);
        when(clusterRepository.findById(anyLong())).thenReturn(Optional.of(cluster));
        when(securityConfigRepository.save(any(SecurityConfig.class))).thenReturn(securityConfig);

        SecurityConfig result = underTest.getSecurityConfig(34L);
        assertEquals(securityConfig.getClientCert(), result.getClientCert());
    }

    @Test
    void testWhenSecurityConfigPresentButValuesNull() {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setServerCert("serverCert");
        securityConfig.setClientKey("clientKey");
        securityConfig.setClientCert("clientCert");
        Cluster cluster = getAutoScaleCluster();
        securityConfig.setCluster(cluster);

        SecurityConfig securityConfig1 = new SecurityConfig();

        when(securityConfigRepository.findByClusterId(anyLong())).thenReturn(securityConfig1);
        when(clusterRepository.findStackCrnById(anyLong())).thenReturn(TEST_CLUSTER_CRN);
        when(cloudbreakCommunicator.getRemoteSecurityConfig(anyString())).thenReturn(securityConfig);
        when(clusterRepository.findById(anyLong())).thenReturn(Optional.of(cluster));
        when(securityConfigRepository.save(any(SecurityConfig.class))).thenReturn(securityConfig);

        SecurityConfig result = underTest.getSecurityConfig(34L);
        assertEquals(securityConfig.getClientCert(), result.getClientCert());
    }

    private Cluster getAutoScaleCluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setId(34L);
        cluster.setStackName(TEST_STACK_NAME);
        cluster.setClusterManager(new ClusterManager());
        return cluster;
    }

}