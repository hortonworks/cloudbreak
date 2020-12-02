package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_AGENT_CRN;
import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_CLUSTER_DOMAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Configs;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyServiceTest {

    private static final long STACK_ID = 100L;

    private static final String TEST_ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    @InjectMocks
    private ClusterProxyService underTest;

    @Mock
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private StackUpdater stackUpdater;

    @Test
    public void testClusterProxyRegisterationWhenCCMV2() {
        Stack aStack = getAStack();
        aStack.setTunnel(Tunnel.CCM);
        aStack.setCcmV2Configs(Json.silent(Map.of(CCMV2_AGENT_CRN, "testAgentCrn", CCMV2_CLUSTER_DOMAIN, "testClusterDomain")));

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(entitlementService.ccmV2Enabled(INTERNAL_ACTOR_CRN, TEST_ACCOUNT_ID)).thenReturn(true);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);

        underTest.registerBootstrapFreeIpa(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should  be enabled.");
        assertEquals(List.of(new CcmV2Configs("testAgentCrn", "testClusterDomain", ServiceFamilies.GATEWAY.getDefaultPort())),
                proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should match");
    }

    @Test
    public void testClusterProxyRegisterationWhenCCMV1() {
        Stack aStack = getAStack();
        aStack.setTunnel(Tunnel.CCM);
        aStack.setMinaSshdServiceId("minaSshdServiceId");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", "privateAddress",
                9443, "instanceId", false);

        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(entitlementService.ccmV2Enabled(INTERNAL_ACTOR_CRN, TEST_ACCOUNT_ID)).thenReturn(false);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);

        underTest.registerBootstrapFreeIpa(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();

        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertEquals(true, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be enabled");
        assertEquals(List.of(new TunnelEntry("instanceId", "GATEWAY", "privateAddress", 9443, "minaSshdServiceId")),
                proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should be configured.");
    }

    @Test
    public void testClusterProxyRegisterationWhenCCMDisabled() {
        Stack aStack = getAStack();

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);

        underTest.registerBootstrapFreeIpa(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        verifyNoInteractions(entitlementService);

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should not be initialized");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be initialized");
    }

    private Stack getAStack() {
        Stack stack = new Stack();
        stack.setAccountId(TEST_ACCOUNT_ID);
        SecurityConfig securityConfig = new SecurityConfig();
        stack.setSecurityConfig(securityConfig);
        return stack;
    }
}
