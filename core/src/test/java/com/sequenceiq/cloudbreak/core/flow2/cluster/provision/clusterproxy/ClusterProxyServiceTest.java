package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Config;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigUpdateRequest;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
class ClusterProxyServiceTest {

    public static final String PRIMARY_INSTANCE_ID = "i-abc123";

    public static final String OTHER_INSTANCE_ID = "i-def456";

    public static final String PRIMARY_PUBLIC_IP = "10.10.10.10";

    public static final String OTHER_PUBLIC_IP = "10.10.10.11";

    private static final long STACK_ID = 100L;

    private static final long CLUSTER_ID = 1000L;

    private static final String TEST_ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:c681a099-bff3-4f3f-8884-1de9604a3a09";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:c681a099-bff3-4f3f-8884-1de9604a3a09";

    private static final String MINA_ID = "mina-id";

    private static final String ENVIRONMENT_CRN = "environment-crn";

    private static final String CLOUDERA_MANAGER_SERVICE = "cloudera-manager";

    @Mock
    private StackService stackService;

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private ClusterProxyService service;

    @Test
    void shouldRegisterProxyConfigurationWithClusterProxy() throws ClusterProxyException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(response);
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ConfigRegistrationResponse registrationResponse =
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> service.registerCluster(testStackUsingCCM()));


        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
        ArgumentCaptor<ConfigRegistrationRequest> configRegistrationRequestArgumentCaptor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(configRegistrationRequestArgumentCaptor.capture());
        ConfigRegistrationRequest requestSent = configRegistrationRequestArgumentCaptor.getValue();
        assertEquals(4, requestSent.getServices().size());
        assertTrue(requestSent.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(requestSent.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(requestSent.getServices().contains(cmServiceConfig()));
        assertTrue(requestSent.getServices().contains(cmInternalServiceConfig()));
    }

    @Test
    void testRegisterClusterWhenCCMV2() throws ClusterProxyException {
        Stack stack = testStack();
        stack.setTunnel(Tunnel.CCMV2);
        stack.setCcmV2AgentCrn("testAgentCrn");
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.registerCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should be enabled");
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.10", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-i-abc123", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.11", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-i-def456", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.10", ServiceFamilies.KNOX.getDefaultPort(), "testAgentCrn-i-abc123", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.11", ServiceFamilies.KNOX.getDefaultPort(), "testAgentCrn-i-def456", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be disabled.");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be configured.");
    }

    @Test
    void testRegisterClusterWhenCCMV2Jumpgate() throws ClusterProxyException {
        Stack stack = testStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        stack.setCcmV2AgentCrn("testAgentCrn");
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.registerCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should be enabled");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should be empty for Jumpgate");
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be disabled.");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be configured.");
    }

    @Test
    void testRegisterClusterWhenCCMV1() throws ClusterProxyException {
        Stack stack = testStackUsingCCM();
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.registerCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should not be initialized.");

        assertEquals(true, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be enabled");
        assertThat(proxyRegisterationReq.getTunnels()).withFailMessage("CCMV1 tunnel should be configured.").hasSameElementsAs(tunnelEntries());
        assertEquals(4, proxyRegisterationReq.getServices().size());
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));
    }

    @Test
    void testReRegisterClusterWhenCCMV2() throws ClusterProxyException {
        Stack stack = testStack();
        stack.setTunnel(Tunnel.CCMV2);
        stack.setCcmV2AgentCrn("testAgentCrn");
        Gateway gateway = new Gateway();
        gateway.setPath("test-cluster");
        stack.getCluster().setGateway(gateway);

        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));
        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.reRegisterCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should be enabled");

        assertEquals("https://10.10.10.10:9443/knox/test-cluster", proxyRegisterationReq.getUriOfKnox(), "CCMV2 Knox URI should match");
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.10", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-i-abc123", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.11", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-i-def456", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.10", ServiceFamilies.KNOX.getDefaultPort(), "testAgentCrn-i-abc123", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getCcmV2Configs().contains(new CcmV2Config(
                "10.10.10.11", ServiceFamilies.KNOX.getDefaultPort(), "testAgentCrn-i-def456", "cloudera-manager")));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be disabled.");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be configured.");
    }

    @Test
    void testReRegisterClusterWhenCCMV2Jumpgate() throws ClusterProxyException {
        Stack stack = testStack();
        stack.setTunnel(Tunnel.CCMV2_JUMPGATE);
        stack.setCcmV2AgentCrn("testAgentCrn");
        Gateway gateway = new Gateway();
        gateway.setPath("test-cluster");
        stack.getCluster().setGateway(gateway);

        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));
        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.reRegisterCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should be enabled");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should be empty for Jumpgate");
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));

        assertEquals("https://10.10.10.10:9443/knox/test-cluster", proxyRegisterationReq.getUriOfKnox(), "CCMV2 Knox URI should match");
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be disabled.");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be configured.");
    }

    @Test
    void testReRegisterClusterWhenCCMV1() throws ClusterProxyException {
        Stack stack = testStackUsingCCM();
        Gateway gateway = new Gateway();
        gateway.setPath("test-cluster");
        stack.getCluster().setGateway(gateway);
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        service.reRegisterCluster(stack);

        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());
        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should not be initialized.");

        assertEquals("https://10.10.10.10/test-cluster", proxyRegisterationReq.getUriOfKnox(), "CCMV1 Knox URI should match");

        assertEquals(true, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be enabled");
        assertThat(proxyRegisterationReq.getTunnels()).withFailMessage("CCMV1 tunnel should be configured.").hasSameElementsAs(tunnelEntries());
        assertEquals(4, proxyRegisterationReq.getServices().size());
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(proxyRegisterationReq.getServices().contains(cmServiceConfig()));
        assertTrue(proxyRegisterationReq.getServices().contains(cmInternalServiceConfig()));
    }

    @Test
    void testRegisterGatewayConfigurationWithoutCcmEnabled() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackWithKnox());

        service.registerGatewayConfiguration(STACK_ID);

        ArgumentCaptor<ConfigUpdateRequest> captor = ArgumentCaptor.forClass(ConfigUpdateRequest.class);
        verify(clusterProxyRegistrationClient).updateConfig(captor.capture());
        ConfigUpdateRequest gatewayUpdateRequest = captor.getValue();
        assertEquals("https://10.10.10.10/test-cluster", gatewayUpdateRequest.getUriOfKnox(), "Gateway Knox URI should match");
    }

    @Test
    void testRegisterGatewayConfigurationWithCcmV1Enabled() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackUsingCCMAndKnox());

        service.registerGatewayConfiguration(STACK_ID);

        ArgumentCaptor<ConfigUpdateRequest> captor = ArgumentCaptor.forClass(ConfigUpdateRequest.class);
        verify(clusterProxyRegistrationClient).updateConfig(captor.capture());
        ConfigUpdateRequest gatewayUpdateRequest = captor.getValue();
        assertEquals("https://10.10.10.10/test-cluster", gatewayUpdateRequest.getUriOfKnox(), "CCMV1 Knox URI should match");
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCMV2", "CCMV2_JUMPGATE"}, mode = EnumSource.Mode.INCLUDE)
    void testRegisterGatewayConfigurationWithCcmV2Enabled(Tunnel tunnel) {
        Stack testStack = testStackUsingCCMAndKnox();
        testStack.setTunnel(tunnel);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStack);

        service.registerGatewayConfiguration(STACK_ID);

        ArgumentCaptor<ConfigUpdateRequest> captor = ArgumentCaptor.forClass(ConfigUpdateRequest.class);
        verify(clusterProxyRegistrationClient).updateConfig(captor.capture());
        ConfigUpdateRequest gatewayUpdateRequest = captor.getValue();
        assertEquals("https://10.10.10.10:9443/knox/test-cluster", gatewayUpdateRequest.getUriOfKnox(), "CCMV2 Knox URI should match");
    }

    @Test
    void shouldNotRegisterSSHTunnelInfoIfCCMIsDisabled() throws ClusterProxyException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(response);
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ConfigRegistrationResponse registrationResponse = service.registerCluster(testStack());
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
        ArgumentCaptor<ConfigRegistrationRequest> configRegistrationRequestArgumentCaptor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(configRegistrationRequestArgumentCaptor.capture());
        ConfigRegistrationRequest requestSent = configRegistrationRequestArgumentCaptor.getValue();
        assertEquals(4, requestSent.getServices().size());
        assertTrue(requestSent.getServices().contains(cmServiceConfigWithInstanceId(PRIMARY_PUBLIC_IP, PRIMARY_INSTANCE_ID)));
        assertTrue(requestSent.getServices().contains(cmServiceConfigWithInstanceId(OTHER_PUBLIC_IP, OTHER_INSTANCE_ID)));
        assertTrue(requestSent.getServices().contains(cmServiceConfig()));
        assertTrue(requestSent.getServices().contains(cmInternalServiceConfig()));
    }

    @Test
    void shouldFailIfVaultSecretIsInvalid() throws ClusterProxyException {
        assertThrows(VaultConfigException.class, () -> service.registerCluster(testStackWithInvalidSecret()));
        verify(clusterProxyRegistrationClient, times(0)).registerConfig(any());
    }

    @Test
    void shouldUpdateKnoxUrlWithClusterProxy() throws ClusterProxyException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackWithKnox());

        ConfigUpdateRequest request = configUpdateRequest(STACK_CRN);

        service.registerGatewayConfiguration(STACK_ID);
        verify(clusterProxyRegistrationClient).updateConfig(request);
    }

    @Test
    void shouldNotUpdateProxyConfigIfClusterIsNotConfiguredWithGateway() throws ClusterProxyException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStack());

        service.registerGatewayConfiguration(STACK_ID);
        verify(clusterProxyRegistrationClient, times(0)).updateConfig(any());
    }

    @Test
    void shouldDeregisterCluster() throws ClusterProxyException {
        service.deregisterCluster(testStack());
        verify(clusterProxyRegistrationClient).deregisterConfig(STACK_CRN);
    }

    private List<TunnelEntry> tunnelEntries() {
        return List.of(new TunnelEntry(PRIMARY_INSTANCE_ID, "GATEWAY", PRIMARY_PUBLIC_IP, 9443, MINA_ID),
                new TunnelEntry(PRIMARY_INSTANCE_ID, "KNOX", PRIMARY_PUBLIC_IP, 443, MINA_ID),
                new TunnelEntry(OTHER_INSTANCE_ID, "GATEWAY", OTHER_PUBLIC_IP, 9443, MINA_ID),
                new TunnelEntry(OTHER_INSTANCE_ID, "KNOX", OTHER_PUBLIC_IP, 443, MINA_ID));
    }

    private ClusterServiceConfig cmServiceConfigWithInstanceId(String ipAddress, String instanceId) {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        ClientCertificate clientCertificate = new ClientCertificate("/cb/test-data/secret/clientKey:secret:base64",
                "/cb/test-data/secret/clientCert:secret:base64");
        return new ClusterServiceConfig("cb-internal-" + instanceId,
                List.of("https://" + ipAddress + ":9443"), null, false, asList(cloudbreakUser, dpUser), clientCertificate, null);
    }

    private ClusterServiceConfig cmServiceConfig() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        return new ClusterServiceConfig(CLOUDERA_MANAGER_SERVICE,
                List.of("https://10.10.10.10/clouderamanager"), null, false, asList(cloudbreakUser, dpUser), null, null);
    }

    private ClusterServiceConfig cmInternalServiceConfig() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        ClientCertificate clientCertificate = new ClientCertificate("/cb/test-data/secret/clientKey:secret:base64",
                "/cb/test-data/secret/clientCert:secret:base64");
        return new ClusterServiceConfig("cb-internal",
                List.of("https://10.10.10.10:9443"), null, false, asList(cloudbreakUser, dpUser), clientCertificate, null);
    }

    private ConfigUpdateRequest configUpdateRequest(String clusterIdentifier) {
        return new ConfigUpdateRequest(clusterIdentifier, "https://10.10.10.10/test-cluster");
    }

    private SecurityConfig gatewaySecurityConfig() {
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "clientKey", new Secret("clientKey", vaultSecretString("clientKey")));
        ReflectionTestUtils.setField(securityConfig, "clientCert", new Secret("clientCert", vaultSecretString("clientCert")));
        return securityConfig;
    }

    private Stack testStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(STACK_CRN);
        stack.setId(STACK_ID);
        stack.setCluster(testCluster());
        stack.setGatewayPort(9443);
        stack.setClusterProxyRegistered(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData primaryInstanceMetaData = new InstanceMetaData();
        primaryInstanceMetaData.setPublicIp(PRIMARY_PUBLIC_IP);
        primaryInstanceMetaData.setInstanceId(PRIMARY_INSTANCE_ID);
        primaryInstanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPublicIp(OTHER_PUBLIC_IP);
        instanceMetaData.setInstanceId(OTHER_INSTANCE_ID);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData, primaryInstanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private Stack testStackUsingCCM() {
        Stack stack = testStack();
        stack.setUseCcm(true);
        stack.setTunnel(Tunnel.CCM);
        stack.setMinaSshdServiceId(MINA_ID);
        return stack;
    }

    private Stack testStackUsingCCMAndKnox() {
        Stack stack = testStackUsingCCM();
        Gateway gateway = new Gateway();
        gateway.setPath("test-cluster");
        stack.getCluster().setGateway(gateway);
        return stack;
    }

    private Stack testStackWithInvalidSecret() {
        Stack stack = testStack();
        ReflectionTestUtils.setField(stack.getCluster(), "cloudbreakAmbariPassword", new Secret("cbpassword", "invalid-vault-string"));
        return stack;
    }

    private Stack testStackWithKnox() {
        Stack stack = testStack();
        Gateway gateway = new Gateway();
        gateway.setPath("test-cluster");
        stack.getCluster().setGateway(gateway);
        return stack;
    }

    private Cluster testCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setCloudbreakUser("cloudbreak");
        ReflectionTestUtils.setField(cluster, "cloudbreakAmbariPassword", new Secret("cbpassword", vaultSecretString("cbpassword")));
        cluster.setDpUser("cmmgmt");
        ReflectionTestUtils.setField(cluster, "dpAmbariPassword", new Secret("dppassword", vaultSecretString("dppassword")));
        return cluster;
    }

    private String vaultSecretString(String password) {
        return JsonUtil.writeValueAsStringSilent(new VaultSecret("test-engine-path", "test-engine-class",
                "/cb/test-data/secret/" + password));
    }
}
