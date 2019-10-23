package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyServiceTest {
    private static final long STACK_ID = 100L;

    private static final long CLUSTER_ID = 1000L;

    private static final String CLUSTER_PROXY_URL = "http://localhost:10080/cluster-proxy";

    private static final String REGISTER_CONFIG_PATH = "/rpc/registerConfig";

    private static final String UPDATE_CONFIG_PATH = "/rpc/updateConfig";

    private static final String REMOVE_CONFIG_PATH = "/rpc/removeConfig";

    @Mock
    private StackService stackService;

    private MockRestServiceServer mockServer;

    private ClusterProxyService service;

    @Before
    public void setup() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new ClusterProxyService(stackService, restTemplate);
        ReflectionTestUtils.setField(service, "clusterProxyUrl", CLUSTER_PROXY_URL);
        ReflectionTestUtils.setField(service, "registerConfigPath", REGISTER_CONFIG_PATH);
        ReflectionTestUtils.setField(service, "updateConfigPath", UPDATE_CONFIG_PATH);
        ReflectionTestUtils.setField(service, "removeConfigPath", REMOVE_CONFIG_PATH);
    }

    @Test
    public void shouldRegisterProxyConfigurationWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)))
                .andExpect(content().json(configRegistrationRequest()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.writeValueAsStringSilent(response)));

        ConfigRegistrationResponse registrationResponse = service.registerCluster(testStack());
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
    }

    @Test
    public void shouldRegisterSSHTunnelInfoWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");

        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)))
                .andExpect(content().json(configRegistrationRequest()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.writeValueAsStringSilent(response)));

        ConfigRegistrationResponse registrationResponse = service.registerCluster(testStack());
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
    }

    @Test
    public void shouldFailIfVaultSecretIsInvalid() throws URISyntaxException {
        mockServer.expect(never(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)));

        assertThrows(VaultConfigException.class, () -> service.registerCluster(testStackWithInvalidSecret()));
    }

    @Test
    public void shouldUpdateKnoxUrlWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackWithKnox());

        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + UPDATE_CONFIG_PATH)))
                .andExpect(content().json(configUpdateRequest("stack-crn")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        service.registerGatewayConfiguration(STACK_ID);
    }

    @Test
    public void shouldNotUpdateProxyConfigIfClusterIsNotConfiguredWithGateway() throws URISyntaxException, JsonProcessingException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStack());
        mockServer.expect(never(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + UPDATE_CONFIG_PATH)))
                .andExpect(content().json(configUpdateRequest(String.valueOf(CLUSTER_ID))))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        service.registerGatewayConfiguration(STACK_ID);
    }

    @Test
    public void shouldDeregisterClsuter() throws URISyntaxException, JsonProcessingException {
        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REMOVE_CONFIG_PATH)))
                .andExpect(content().json(JsonUtil.writeValueAsStringSilent(of("clusterCrn", "stack-crn"))))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        service.deregisterCluster(testStack());
    }

    private String configRegistrationRequest() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        ClusterServiceConfig service = new ClusterServiceConfig("cloudera-manager",
                List.of("https://10.10.10.10/clouderamanager"), asList(cloudbreakUser, dpUser));
        List<TunnelEntry> tunnelEntries = List.of(new TunnelEntry("i-123foobar", "KNOX", "10.10.10.10", 443));
        ConfigRegistrationRequest request = new ConfigRegistrationRequest("stack-crn", tunnelEntries, List.of(String.valueOf(CLUSTER_ID)), List.of(service));
        return JsonUtil.writeValueAsStringSilent(request);
    }

    private String configUpdateRequest(String clusterIdentifier) {
        return JsonUtil.writeValueAsStringSilent(of("clusterCrn", clusterIdentifier,
                "uriOfKnox", "https://10.10.10.10:8443/test-cluster"));
    }

    @After
    public void teardown() {
        mockServer.verify();
    }

    private Stack testStack() {
        Stack stack = new Stack();
        stack.setResourceCrn("stack-crn");
        stack.setId(STACK_ID);
        stack.setCluster(testCluster());
        stack.setGatewayPort(9443);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData primaryInstanceMetaData = new InstanceMetaData();
        primaryInstanceMetaData.setPublicIp("10.10.10.10");
        primaryInstanceMetaData.setInstanceId("i-123foobar");
        primaryInstanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPublicIp("10.10.10.11");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData, primaryInstanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
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
