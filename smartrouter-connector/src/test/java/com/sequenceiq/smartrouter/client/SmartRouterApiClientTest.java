package com.sequenceiq.smartrouter.client;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static com.sequenceiq.smartrouter.client.SmartRouterServiceConfig.PROXY_WITH_TIMEOUT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import com.cloudera.smartrouter.client.ApiClient;
import com.cloudera.smartrouter.client.api.DefaultApi;
import com.cloudera.smartrouter.client.api.PoliciesApi;
import com.cloudera.smartrouter.client.model.CloneRequest;
import com.cloudera.smartrouter.client.model.HealthResponse;
import com.cloudera.smartrouter.client.model.PolicyPatch;
import com.cloudera.smartrouter.client.model.PolicyPatchMetadata;
import com.cloudera.smartrouter.client.model.PolicyPatchSpec;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;

class SmartRouterApiClientTest {

    private static final String CLUSTER_PROXY_URL = "http://localhost:10180/cluster-proxy";

    private static final String CLUSTER_CRN = "crn:cdp:hybrid:us-west-1:accountId:cluster:clusterId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:userId";

    private static final String SERVICE_NAME = "smart-router";

    // relative to the root URI of the cluster proxy RestTemplate, as RootUriRequestExpectationManager matches requests that way
    private static final String SERVICE_PATH = "/" + CLUSTER_CRN + "/" + SERVICE_NAME;

    private static final String SERVICE_URL = CLUSTER_PROXY_URL + "/proxy" + SERVICE_PATH;

    private MockServerRestTemplateCustomizer mockServerCustomizer;

    private ClusterProxyProxyClientFactory clusterProxyProxyClientFactory;

    @BeforeEach
    void setUp() {
        ClusterProxyConfiguration clusterProxyConfiguration = new ClusterProxyConfiguration();
        clusterProxyConfiguration.setClusterProxyUrl(CLUSTER_PROXY_URL);
        clusterProxyConfiguration.setHttpProxyPath("/proxy");

        SmartRouterServiceConfig smartRouterServiceConfig = new SmartRouterServiceConfig();
        ReflectionTestUtils.setField(smartRouterServiceConfig, "connectTimeoutSec", 5L);
        ReflectionTestUtils.setField(smartRouterServiceConfig, "readTimeoutSec", 30L);

        mockServerCustomizer = new MockServerRestTemplateCustomizer();
        clusterProxyProxyClientFactory = smartRouterServiceConfig.smartRouterClusterProxyProxyClientFactory(clusterProxyConfiguration,
                new RestTemplateBuilder(mockServerCustomizer));
    }

    @Test
    void testCreateRoutesThroughClusterProxyWithActorCrn() {
        ApiClient apiClient = new SmartRouterApiClient(clusterProxyProxyClientFactory.create(CLUSTER_CRN, SERVICE_NAME, USER_CRN));
        MockRestServiceServer server = mockServerCustomizer.getServer();
        server.expect(requestTo(SERVICE_PATH + "/v1/health"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(ACTOR_CRN_HEADER, USER_CRN))
                .andExpect(header(PROXY_WITH_TIMEOUT_HEADER, "30000"))
                .andExpect(request -> assertThat(request.getHeaders().getFirst(REQUEST_ID_HEADER)).isNotBlank())
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        HealthResponse response = new DefaultApi(apiClient).serviceHealth();

        assertThat(apiClient.getBasePath()).isEqualTo(SERVICE_URL);
        assertThat(response.getStatus()).isEqualTo("UP");
        server.verify();
    }

    @Test
    void testCustomMethodPathIsResolvedAgainstClusterProxy() {
        ApiClient apiClient = new SmartRouterApiClient(clusterProxyProxyClientFactory.create(CLUSTER_CRN, SERVICE_NAME, USER_CRN));
        MockRestServiceServer server = mockServerCustomizer.getServer();
        server.expect(requestTo(SERVICE_PATH + "/v1/policies/source-policy:clone"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(ACTOR_CRN_HEADER, USER_CRN))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new PoliciesApi(apiClient).clonePolicy("source-policy", new CloneRequest(), null);

        server.verify();
    }

    @Test
    void testErrorResponseIsTranslatedToClusterProxyException() {
        ApiClient apiClient = new SmartRouterApiClient(clusterProxyProxyClientFactory.create(CLUSTER_CRN, SERVICE_NAME, USER_CRN));
        MockRestServiceServer server = mockServerCustomizer.getServer();
        server.expect(requestTo(SERVICE_PATH + "/v1/health"))
                .andRespond(withServerError().body("{\"error\":\"policy store unavailable\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new DefaultApi(apiClient).serviceHealth())
                .isInstanceOf(ClusterProxyException.class)
                .hasMessage("Error getting response for cluster identifier '" + CLUSTER_CRN + "' with cluster proxy configuration for service " +
                        "'smart-router', Error Response Body '{\"error\":\"policy store unavailable\"}'");
        server.verify();
    }

    @Test
    void testConnectionFailureIsTranslatedToClusterProxyException() {
        ApiClient apiClient = new SmartRouterApiClient(clusterProxyProxyClientFactory.create(CLUSTER_CRN, SERVICE_NAME, USER_CRN));
        MockRestServiceServer server = mockServerCustomizer.getServer();
        server.expect(requestTo(SERVICE_PATH + "/v1/health"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> new DefaultApi(apiClient).serviceHealth())
                .isInstanceOf(ClusterProxyException.class)
                .hasMessage("Error getting response for cluster identifier '" + CLUSTER_CRN + "' with cluster proxy configuration for service " +
                        "'smart-router'")
                .hasRootCauseInstanceOf(IOException.class);
        server.verify();
    }

    @Test
    void testPolicyPatchSendsOnlyTheChangedSpecField() {
        assertPolicyPatchBody(new PolicyPatch().spec(new PolicyPatchSpec().enabled(false)), "{\"spec\":{\"enabled\":false}}");
    }

    @Test
    void testPolicyPatchSendsOnlyTheChangedMetadataField() {
        assertPolicyPatchBody(new PolicyPatch().metadata(new PolicyPatchMetadata().displayName("Renamed policy")),
                "{\"metadata\":{\"displayName\":\"Renamed policy\"}}");
    }

    @Test
    void testPolicyPatchSendsTheClearedFields() {
        assertPolicyPatchBody(new PolicyPatch()
                        .metadata(new PolicyPatchMetadata().description(null))
                        .spec(new PolicyPatchSpec().scopes(null).targets(List.of())),
                "{\"metadata\":{\"description\":null},\"spec\":{\"scopes\":null,\"targets\":[]}}");
    }

    private void assertPolicyPatchBody(PolicyPatch policyPatch, String expectedBody) {
        ApiClient apiClient = new SmartRouterApiClient(clusterProxyProxyClientFactory.create(CLUSTER_CRN, SERVICE_NAME, USER_CRN));
        MockRestServiceServer server = mockServerCustomizer.getServer();
        server.expect(requestTo(SERVICE_PATH + "/v1/policies/policy"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().contentType("application/merge-patch+json"))
                .andExpect(content().json(expectedBody, JsonCompareMode.STRICT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        new PoliciesApi(apiClient).patchPolicy("policy", "\"1\"", policyPatch, null, null);

        server.verify();
    }
}
