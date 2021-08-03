package com.sequenceiq.cloudbreak.clusterproxy;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class ClusterProxyRegistrationClientTest {
    private static final String STACK_CRN = "stack-crn";

    private static final String ENVIORONMENT_CRN = "environment-crn";

    private static final String CLUSTER_ID = "cluster-id";

    private static final String KNOX_URI = "https://10.10.10.10:8443/test-cluster";

    private static final String CLUSTER_PROXY_URL = "http://localhost:10080/cluster-proxy";

    private static final String REGISTER_CONFIG_PATH = "/rpc/registerConfig";

    private static final String UPDATE_CONFIG_PATH = "/rpc/updateConfig";

    private static final String REMOVE_CONFIG_PATH = "/rpc/removeConfig";

    private static final List<String> CERTIFICATES = asList("certificate1", "certificate2");

    private MockRestServiceServer mockServer;

    private ClusterProxyRegistrationClient service;

    @Before
    public void setup() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new ClusterProxyRegistrationClient(restTemplate);

        ClusterProxyConfiguration proxyConfig = spy(ClusterProxyConfiguration.class);
        ReflectionTestUtils.setField(proxyConfig, "clusterProxyUrl", CLUSTER_PROXY_URL);
        ReflectionTestUtils.setField(proxyConfig, "registerConfigPath", REGISTER_CONFIG_PATH);
        ReflectionTestUtils.setField(proxyConfig, "updateConfigPath", UPDATE_CONFIG_PATH);
        ReflectionTestUtils.setField(proxyConfig, "removeConfigPath", REMOVE_CONFIG_PATH);

        ReflectionTestUtils.setField(service, "clusterProxyConfiguration", proxyConfig);
    }

    @Test
    public void shouldRegisterProxyConfigurationWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        ClusterServiceConfig clusterServiceConfig = clusterServiceConfig();
        ConfigRegistrationRequest request = configRegistrationRequest(STACK_CRN, ENVIORONMENT_CRN, CLUSTER_ID, clusterServiceConfig, CERTIFICATES);

        ConfigRegistrationResponse response = new ConfigRegistrationResponse();
        response.setX509Unwrapped("X509PublicKey");
        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REGISTER_CONFIG_PATH)))
                .andExpect(content().json(JsonUtil.writeValueAsStringSilent(request)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtil.writeValueAsStringSilent(response)));

        ConfigRegistrationResponse registrationResponse = service.registerConfig(request);
        assertEquals("X509PublicKey", registrationResponse.getX509Unwrapped());
    }

    @Test
    public void shouldUpdateKnoxUrlWithClusterProxy() throws URISyntaxException, JsonProcessingException {
        ConfigUpdateRequest request = configUpdateRequest(STACK_CRN, KNOX_URI);
        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + UPDATE_CONFIG_PATH)))
                .andExpect(content().json(JsonUtil.writeValueAsStringSilent(request)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        service.updateConfig(request);
    }

    @Test
    public void shouldDeregisterCluster() throws URISyntaxException, JsonProcessingException {
        ConfigDeleteRequest request = new ConfigDeleteRequest(STACK_CRN);
        mockServer.expect(once(), MockRestRequestMatchers.requestTo(new URI(CLUSTER_PROXY_URL + REMOVE_CONFIG_PATH)))
                .andExpect(content().json(JsonUtil.writeValueAsStringSilent(request)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK));

        service.deregisterConfig(STACK_CRN);
    }

    private ClusterServiceConfig clusterServiceConfig() {
        ClusterServiceCredential cloudbreakUser = new ClusterServiceCredential("cloudbreak", "/cb/test-data/secret/cbpassword:secret");
        ClusterServiceCredential dpUser = new ClusterServiceCredential("cmmgmt", "/cb/test-data/secret/dppassword:secret", true);
        return new ClusterServiceConfig("cloudera-manager",
                List.of("https://10.10.10.10/clouderamanager"), null, false, asList(cloudbreakUser, dpUser), null, null);
    }

    private ConfigRegistrationRequest configRegistrationRequest(String stackCrn, String environmentCrn, String clusterId,
                                                                ClusterServiceConfig serviceConfig, List<String> certificates) {
        return new ConfigRegistrationRequestBuilder(stackCrn).withEnvironmentCrn(environmentCrn)
                .withAliases(List.of(clusterId)).withServices(List.of(serviceConfig))
                .withCertificates(certificates).build();
    }

    private ConfigUpdateRequest configUpdateRequest(String stackCrn, String knoxUri) {
        return new ConfigUpdateRequest(stackCrn, knoxUri);
    }

    @After
    public void teardown() {
        mockServer.verify();
    }
}
