package com.sequenceiq.freeipa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;

@ExtendWith(MockitoExtension.class)
class GatewayConfigServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private GatewayConfigService underTest;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private SaltService saltService;

    @Test
    void testGetPrimaryGatewayConfig() {
        Stack stack = stack();
        InstanceMetaData instance = instance();
        when(instanceMetaDataRepository.findNotTerminatedForStack(eq(STACK_ID))).thenReturn(Set.of(instance));
        GatewayConfig gatewayConfig =  GatewayConfig.builder()
                .withConnectionAddress("host")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withPrimary(true)
                .withHostname("host")
                .withInstanceId("instanceId")
                .withKnoxGatewayEnabled(false)
                .build();
        when(tlsSecurityService.buildGatewayConfig(eq(stack()), eq(instance), any(), anyBoolean())).thenReturn(gatewayConfig);

        GatewayConfig result = underTest.getPrimaryGatewayConfig(stack);

        assertEquals(gatewayConfig, result);
        verify(instanceMetaDataRepository, times(1)).findNotTerminatedForStack(eq(STACK_ID));
        verify(tlsSecurityService, times(1)).buildGatewayConfig(eq(stack), eq(instance), any(), anyBoolean());
    }

    @Test
    void testGetPrimaryGatewayConfigForSalt() throws CloudbreakOrchestratorFailedException {
        Stack stack = stack();
        InstanceMetaData instance = instance();
        when(instanceMetaDataRepository.findNotTerminatedForStack(eq(STACK_ID))).thenReturn(Set.of(instance));
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("host")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withPrimary(true)
                .withHostname("host")
                .withInstanceId("instanceId1")
                .withKnoxGatewayEnabled(false)
                .build();
        GatewayConfig overriddenGatewayConfig =  GatewayConfig.builder()
                .withConnectionAddress("host")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withPrimary(true)
                .withHostname("host")
                .withInstanceId("instanceId2")
                .withKnoxGatewayEnabled(false)
                .build();
        when(tlsSecurityService.buildGatewayConfig(eq(stack()), eq(instance), any(), anyBoolean())).thenReturn(gatewayConfig);
        when(saltService.getPrimaryGatewayConfig(anyList())).thenReturn(overriddenGatewayConfig);

        GatewayConfig result = underTest.getPrimaryGatewayConfigForSalt(stack);

        assertEquals(overriddenGatewayConfig, result);
    }

    private Stack stack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        SecurityConfig securityConfig = new SecurityConfig();
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltBootSignPrivateKeyVault("saltBootSignPrivateKey");
        saltSecurityConfig.setSaltBootPasswordVault("saltBootPassword");
        saltSecurityConfig.setSaltPasswordVault("saltPassword");
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        stack.setSecurityConfig(securityConfig);
        return stack;
    }

    private InstanceMetaData instance() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        return instanceMetaData;
    }

}