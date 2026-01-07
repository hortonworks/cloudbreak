package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.SaltClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class TlsSecurityServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private TlsSecurityService underTest;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Test
    void testBuildGatewayConfigWhenNoSaltMasterKeyPairPresent() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);

        InstanceMetadataView gatewayInstance = mock(InstanceMetadataView.class);
        when(securityConfigService.findOneByStackId(eq(STACK_ID))).thenReturn(Optional.of(securityConfig(null)));
        when(clusterProxyService.isCreateConfigForClusterProxy(any())).thenReturn(Boolean.FALSE);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(Boolean.FALSE);
        GatewayConfig gatewayConfig = underTest.buildGatewayConfig(stack, gatewayInstance, 1, new SaltClientConfig(null, null, null), false);
        assertNull(gatewayConfig.getSaltMasterPrivateKey());
        assertNull(gatewayConfig.getSaltMasterPublicKey());
    }

    @Test
    void testBuildGatewayConfigWhenSaltMasterKeyPairPresent() {
        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);

        InstanceMetadataView gatewayInstance = mock(InstanceMetadataView.class);
        SecurityConfig securityConfig = securityConfig(PkiUtil.generatePemPrivateKeyInBase64());
        when(securityConfigService.findOneByStackId(eq(STACK_ID))).thenReturn(Optional.of(securityConfig));
        when(clusterProxyService.isCreateConfigForClusterProxy(any())).thenReturn(Boolean.FALSE);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(Boolean.FALSE);
        GatewayConfig gatewayConfig = underTest.buildGatewayConfig(stack, gatewayInstance, 1, new SaltClientConfig(null, null, null), false);
        assertNotNull(gatewayConfig.getSaltMasterPrivateKey());
        assertNotNull(gatewayConfig.getSaltMasterPublicKey());
    }

    private SecurityConfig securityConfig(String saltMasterPrivateKey) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setClientKey("clientKey");
        securityConfig.setClientCert("clientCert");
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltSignPrivateKey(PkiUtil.generatePemPrivateKeyInBase64());
        saltSecurityConfig.setSaltMasterPrivateKey(saltMasterPrivateKey);
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        return securityConfig;
    }

}