package com.sequenceiq.freeipa.service.stack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Config;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceHealthCheck;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigService;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.polling.clusterproxy.ServiceEndpointHealthPollerObject;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;
import com.sequenceiq.freeipa.util.HealthCheckAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
public class ClusterProxyServiceTest {

    private static final long STACK_ID = 100L;

    private static final String TEST_ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String FREEIPA_SERVICE = "freeipa";

    private static final String PRIVATE_IP_ADDRESS = "privateIpAddress";

    private static final String PRIVATE_IP_ADDRESS_1 = "privateIpAddress1";

    private static final String PRIVATE_IP_ADDRESS_2 = "privateIpAddress2";

    private static final String PRIVATE_ADDRESS = "privateAddress";

    private static final int INTERVAL_IN_SEC_V_2 = 12;

    private static final String HEALTH_STATUS_ENDPOINT_V_2 = "health.info";

    private static final int TIMEOUT_IN_SEC_V_2 = 34;

    private static final int HEALTHY_STATUS_CODE_V_2 = 200;

    private static final String STACK_RESOURCE_CRN = "resourceCrn";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

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
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private ClusterProxyServiceAvailabilityChecker clusterProxyServiceAvailabilityChecker;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private PollingService<ServiceEndpointHealthPollerObject> serviceEndpointHealthPollingService;

    @Mock
    private HealthCheckAvailabilityChecker healthCheckAvailabilityChecker;

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCMV2", "CCMV2_JUMPGATE"}, mode = EnumSource.Mode.INCLUDE)
    public void testClusterProxyRegistrationWhenCCMV2OrJumpgate(Tunnel ccmv2Mode) {
        Stack aStack = getAStack();
        aStack.setTunnel(ccmv2Mode);
        aStack.setCcmV2AgentCrn("testAgentCrn");
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicIpAddress", PRIVATE_IP_ADDRESS,
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId", true);
        ConfigRegistrationResponse configRegResponse = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResponse);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegistrationReq = captor.getValue();

        assertThat(proxyRegistrationReq.getClusterCrn()).isEqualTo(STACK_RESOURCE_CRN);
        assertThat(proxyRegistrationReq.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        assertFalse(proxyRegistrationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertTrue(proxyRegistrationReq.isUseCcmV2(), ccmv2Mode + " should be enabled.");
        assertEquals(List.of(new CcmV2Config("testAgentCrn", PRIVATE_IP_ADDRESS, ServiceFamilies.GATEWAY.getDefaultPort(),
                        "testAgentCrn-testInstanceId", FREEIPA_SERVICE)),
                proxyRegistrationReq.getCcmV2Configs(), ccmv2Mode + " config should match");

        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa", List.of("https://privateIpAddress:9443"), List.of(), null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa.ipadom", List.of("https://privateIpAddress:9443"), List.of(),
                null));
        assertThat(proxyRegistrationReq.getEnvironmentCrn()).isEqualTo(ENVIRONMENT_CRN);
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCMV2", "CCMV2_JUMPGATE"}, mode = EnumSource.Mode.INCLUDE)
    public void testUpdateClusterProxyRegistrationWhenCCMV2OrJumpgate(Tunnel ccmv2Mode) {
        Stack aStack = getAStack();
        aStack.setTunnel(ccmv2Mode);
        aStack.setCcmV2AgentCrn("testAgentCrn");

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUsePrivateIpToTls(true);
        aStack.setSecurityConfig(securityConfig);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("test.freeipa.domain");

        GatewayConfig primaryGateway = new GatewayConfig("primaryAddress", "primaryPublicAddress", "primaryPrivateAddress",
                ServiceFamilies.GATEWAY.getDefaultPort(), "privateInstanceId", true);
        GatewayConfig gatewayConfig1 = new GatewayConfig("connectionAddress1", "publicIpAddress1", PRIVATE_IP_ADDRESS_1,
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId1", true);
        ReflectionTestUtils.setField(gatewayConfig1, "hostname", "hostname1");
        GatewayConfig gatewayConfig2 = new GatewayConfig("connectionAddress2", "publicIpAddress2", PRIVATE_IP_ADDRESS_2,
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId2", true);
        ReflectionTestUtils.setField(gatewayConfig2, "hostname", "hostname2");
        ConfigRegistrationResponse configRegResponse = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(primaryGateway);
        when(gatewayConfigService.getNotDeletedGatewayConfigs(aStack)).thenReturn(List.of(gatewayConfig1, gatewayConfig2));
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResponse);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);
        when(clusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(aStack)).thenReturn(true);
        when(serviceEndpointHealthPollingService.pollWithTimeout(any(), any(), anyLong(), anyInt(), anyInt())).thenReturn(null);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(aStack)).thenReturn(true);

        ReflectionTestUtils.setField(underTest, "intervalInSecV2", INTERVAL_IN_SEC_V_2);
        ReflectionTestUtils.setField(underTest, "healthStatusEndpointV2", HEALTH_STATUS_ENDPOINT_V_2);
        ReflectionTestUtils.setField(underTest, "timeoutInSecV2", TIMEOUT_IN_SEC_V_2);
        ReflectionTestUtils.setField(underTest, "healthyStatusCodeV2", HEALTHY_STATUS_CODE_V_2);

        underTest.updateFreeIpaRegistrationAndWait(STACK_ID, List.of("testInstanceId1", "testInstanceId2"));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegistrationReq = captor.getValue();

        assertThat(proxyRegistrationReq.getClusterCrn()).isEqualTo(STACK_RESOURCE_CRN);
        assertThat(proxyRegistrationReq.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        assertFalse(proxyRegistrationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertTrue(proxyRegistrationReq.isUseCcmV2(), ccmv2Mode + " should be enabled.");
        assertEquals(List.of(
                new CcmV2Config("testAgentCrn", PRIVATE_IP_ADDRESS_1, ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-testInstanceId1", FREEIPA_SERVICE
                ),
                new CcmV2Config("testAgentCrn", PRIVATE_IP_ADDRESS_2, ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-testInstanceId2", FREEIPA_SERVICE
                )), proxyRegistrationReq.getCcmV2Configs(), ccmv2Mode + " config should match");

        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa", List.of("https://primaryPrivateAddress:9443"), List.of(),
                null));
        assertThat(proxyRegistrationReq.getServices()).doesNotContain(new ClusterServiceConfig("freeipa.test.freeipa.domain",
                List.of("https://primaryPrivateAddress:9443"), List.of(), null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("hostname1", List.of("https://privateIpAddress1:9443"), List.of(),
                null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("hostname2", List.of("https://privateIpAddress2:9443"), List.of(),
                null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa.test.freeipa.domain",
                List.of("https://privateIpAddress1:9443", "https://privateIpAddress2:9443"), null, false, List.of(), null,
                new ClusterServiceHealthCheck(INTERVAL_IN_SEC_V_2, HEALTH_STATUS_ENDPOINT_V_2, TIMEOUT_IN_SEC_V_2, HEALTHY_STATUS_CODE_V_2)));
        assertThat(proxyRegistrationReq.getEnvironmentCrn()).isEqualTo(ENVIRONMENT_CRN);
    }

    @Test
    public void testClusterProxyRegistrationWhenCCMV1() {
        Stack aStack = getAStack();
        aStack.setTunnel(Tunnel.CCM);
        aStack.setMinaSshdServiceId("minaSshdServiceId");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", PRIVATE_ADDRESS,
                9443, "instanceId", false);

        ConfigRegistrationResponse configRegResponse = mock(ConfigRegistrationResponse.class);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResponse);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegistrationReq = captor.getValue();

        assertThat(proxyRegistrationReq.getClusterCrn()).isEqualTo(STACK_RESOURCE_CRN);
        assertThat(proxyRegistrationReq.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        assertFalse(proxyRegistrationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertTrue(proxyRegistrationReq.isUseTunnel(), "CCMV1 tunnel should be enabled");
        assertEquals(List.of(new TunnelEntry("instanceId", "GATEWAY", PRIVATE_ADDRESS, 9443, "minaSshdServiceId")),
                proxyRegistrationReq.getTunnels(), "CCMV1 tunnel should be configured.");

        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa", List.of("https://privateAddress:9443"), List.of(), null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa.ipadom", List.of("https://privateAddress:9443"), List.of(),
                null));
    }

    @Test
    public void testClusterProxyRegistrationWhenCCMDisabled() {
        Stack aStack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", PRIVATE_ADDRESS,
                9443, "instanceId", false);
        ConfigRegistrationResponse configRegResponse = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResponse);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegistrationReq = captor.getValue();

        assertThat(proxyRegistrationReq.getClusterCrn()).isEqualTo(STACK_RESOURCE_CRN);
        assertThat(proxyRegistrationReq.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        assertFalse(proxyRegistrationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertFalse(proxyRegistrationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertNull(proxyRegistrationReq.getCcmV2Configs(), "CCMV2 config should not be initialized");
        assertNull(proxyRegistrationReq.getTunnels(), "CCMV1 tunnel should not be initialized");

        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa", List.of("https://publicAddress:9443"), List.of(), null));
        assertThat(proxyRegistrationReq.getServices()).contains(new ClusterServiceConfig("freeipa.ipadom", List.of("https://publicAddress:9443"), List.of(),
                null));
    }

    @Test
    public void testGetPath() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");

        String result = underTest.getProxyPath("testCrn");

        assertEquals("basePath/proxy/testCrn/freeipa", result);
    }

    @Test
    public void testGetPathWithStackWithoutServiceName() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        String result = underTest.getProxyPath(stack, null);

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameNotRegistered() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        readConfigResponse.setServices(List.of());
        when(clusterProxyRegistrationClient.readConfig(STACK_RESOURCE_CRN)).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, "unregistered");

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameNotRegisteredWithEmptyServices() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        when(clusterProxyRegistrationClient.readConfig(STACK_RESOURCE_CRN)).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, "unregistered");

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameRegistered() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        ReadConfigService service = new ReadConfigService();
        service.setName("registered");
        readConfigResponse.setServices(List.of(service));
        when(clusterProxyRegistrationClient.readConfig(STACK_RESOURCE_CRN)).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, "registered");

        assertEquals("basePath/proxy/resourceCrn/registered", result);
    }

    private Stack getAStack() {
        Stack stack = new Stack();
        stack.setAccountId(TEST_ACCOUNT_ID);
        stack.setResourceCrn(STACK_RESOURCE_CRN);
        SecurityConfig securityConfig = new SecurityConfig();
        stack.setSecurityConfig(securityConfig);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        return stack;
    }

}
