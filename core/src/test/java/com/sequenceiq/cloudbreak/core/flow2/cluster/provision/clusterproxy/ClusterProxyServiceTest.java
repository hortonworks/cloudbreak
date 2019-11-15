package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigUpdateRequest;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyServiceTest {
    private static final long STACK_ID = 100L;

    private static final long CLUSTER_ID = 1000L;

    private static final String TEST_ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:cluster:c681a099-bff3-4f3f-8884-1de9604a3a09";

    @Mock
    private StackService stackService;

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private SecurityConfigService securityConfigService;

    private ClusterProxyService service;

    @Before
    public void setup() {
        service = new ClusterProxyService(stackService, clusterProxyRegistrationClient, securityConfigService);
    }

    @Test
    public void shouldRegisterProxyConfigurationWithClusterProxy() throws ClusterProxyException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        ConfigRegistrationRequest request = configRegistrationRequestWithTunnelEntries();

        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(response);
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ConfigRegistrationResponse registrationResponse = service.registerCluster(testStackUsingCCM());
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
        verify(clusterProxyRegistrationClient).registerConfig(request);
    }

    @Test
    public void shouldNotRegisterSSHTunnelInfoIfCCMIsDisabled() throws ClusterProxyException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        ConfigRegistrationRequest request = configRegistrationRequest();

        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(response);
        when(securityConfigService.findOneByStackId(STACK_ID)).thenReturn(Optional.of(gatewaySecurityConfig()));

        ConfigRegistrationResponse registrationResponse = service.registerCluster(testStack());
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
        verify(clusterProxyRegistrationClient).registerConfig(request);
    }

    @Test
    public void shouldFailIfVaultSecretIsInvalid() throws ClusterProxyException {
        assertThrows(VaultConfigException.class, () -> service.registerCluster(testStackWithInvalidSecret()));
        verify(clusterProxyRegistrationClient, times(0)).registerConfig(any());
    }

    @Test
    public void shouldUpdateKnoxUrlWithClusterProxy() throws ClusterProxyException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackWithKnox());

        ConfigUpdateRequest request = configUpdateRequest(STACK_CRN);

        service.registerGatewayConfiguration(STACK_ID);
        verify(clusterProxyRegistrationClient).updateConfig(request);
    }

    @Test
    public void shouldNotUpdateProxyConfigIfClusterIsNotConfiguredWithGateway() throws ClusterProxyException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStack());

        service.registerGatewayConfiguration(STACK_ID);
        verify(clusterProxyRegistrationClient, times(0)).updateConfig(any());
    }

    @Test
    public void shouldDeregisterCluster() throws ClusterProxyException {
        service.deregisterCluster(testStack());
        verify(clusterProxyRegistrationClient).deregisterConfig(STACK_CRN);
    }

    private ConfigRegistrationRequest configRegistrationRequest() {
        return new ConfigRegistrationRequestBuilder(STACK_CRN)
                .withAliases(List.of(String.valueOf(CLUSTER_ID))).withServices(List.of(cmServiceConfig(), cmInternalServiceConfig())).build();
    }

    private ConfigRegistrationRequest configRegistrationRequestWithTunnelEntries() {
        List<TunnelEntry> tunnelEntries = List.of(new TunnelEntry("i-abc123", "GATEWAY", "10.10.10.10", 9443),
                new TunnelEntry("i-abc123", "KNOX", "10.10.10.10", 443));
        return new ConfigRegistrationRequestBuilder(STACK_CRN)
                .withAliases(List.of(String.valueOf(CLUSTER_ID))).withServices(List.of(cmServiceConfig(), cmInternalServiceConfig()))
                .withAccountId(TEST_ACCOUNT_ID)
                .withTunnelEntries(tunnelEntries).build();
    }

    private ClusterServiceConfig cmServiceConfig() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        return new ClusterServiceConfig("cloudera-manager",
                List.of("https://10.10.10.10/clouderamanager"), asList(cloudbreakUser, dpUser), null, null);
    }

    private ClusterServiceConfig cmInternalServiceConfig() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        ClientCertificate clientCertificate = new ClientCertificate("/cb/test-data/secret/clientKey:secret",
                "/cb/test-data/secret/clientCert:secret");
        return new ClusterServiceConfig("cloudera-manager-internal",
                List.of("https://10.10.10.10:9443/"), asList(cloudbreakUser, dpUser), clientCertificate, null);
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
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData primaryInstanceMetaData = new InstanceMetaData();
        primaryInstanceMetaData.setPublicIp("10.10.10.10");
        primaryInstanceMetaData.setInstanceId("i-abc123");
        primaryInstanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPublicIp("10.10.10.11");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData, primaryInstanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private Stack testStackUsingCCM() {
        Stack stack = testStack();
        stack.setUseCcm(true);
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
        cluster.setCloudbreakAmbariUser("cloudbreak");
        ReflectionTestUtils.setField(cluster, "cloudbreakAmbariPassword", new Secret("cbpassword", vaultSecretString("cbpassword")));
        cluster.setDpAmbariUser("cmmgmt");
        ReflectionTestUtils.setField(cluster, "dpAmbariPassword", new Secret("dppassword", vaultSecretString("dppassword")));
        return cluster;
    }

    private String vaultSecretString(String password) {
        return JsonUtil.writeValueAsStringSilent(new VaultSecret("test-engine-path", "test-engine-class",
                "/cb/test-data/secret/" + password));
    }
}
