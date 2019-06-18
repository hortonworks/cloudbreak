package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

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
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyServiceTest {
    private static final long STACK_ID = 100L;

    private static final long CLUSTER_ID = 1000L;

    private static final String CLUSTER_PROXY_URL = "http://localhost:10080/cluster-proxy";

    private static final String REGISTER_CONFIG_PATH = "/rpc/registerConfig";

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
    }

    @Test
    public void shouldRegisterProxyConfigurationWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStack());
        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setId("123");
        response.setKey("X509PublicKey");
        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)))
                .andExpect(content().json(configRegistrationRequest()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.writeValueAsStringSilent(response)));

        ConfigRegistrationResponse registrationResponse = service.registerProxyConfiguration(STACK_ID);
        assertEquals("123", registrationResponse.getId());
        assertEquals("X509PublicKey", registrationResponse.getKey());
    }

    private String configRegistrationRequest() {
        ClusterServiceCredential credential1 = new ClusterServiceCredential("cbuser", "/cb/test-data/secret/cbpassword");
        ClusterServiceCredential credential2 = new ClusterServiceCredential("dpuser", "/cb/test-data/secret/dppassword");
        ClusterServiceConfig service = new ClusterServiceConfig("cloudera-manager",
                List.of("https://10.10.10.10:8443"), asList(credential1, credential2));
        return JsonUtil.writeValueAsStringSilent(new ConfigRegistrationRequest("1000", List.of(service)));
    }

    @Test
    public void shouldFailIfVaultSecretIsInvalid() throws URISyntaxException {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(testStackWithInvalidSecret());
        mockServer.expect(never(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)));

        assertThrows(VaultConfigException.class, () -> service.registerProxyConfiguration(STACK_ID));
    }

    @After
    public void teardown() {
        mockServer.verify();
    }

    private Stack testStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setCluster(testCluster());
        stack.setGatewayPort(8443);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData primaryInstanceMetaData = new InstanceMetaData();
        primaryInstanceMetaData.setPublicIp("10.10.10.10");
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

    private Cluster testCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setCloudbreakAmbariUser("cbuser");
        ReflectionTestUtils.setField(cluster, "cloudbreakAmbariPassword", new Secret("cbpassword", vaultSecretString("cbpassword")));
        cluster.setDpAmbariUser("dpuser");
        ReflectionTestUtils.setField(cluster, "dpAmbariPassword", new Secret("dppassword", vaultSecretString("dppassword")));
        return cluster;
    }

    private String vaultSecretString(String password) {
        return JsonUtil.writeValueAsStringSilent(new VaultSecret("test-engine-path", "test-engine-class",
                "/cb/test-data/secret/" + password));
    }
}