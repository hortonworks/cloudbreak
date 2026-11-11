package com.sequenceiq.smartrouter.client;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.smartrouter.client.api.AuthoringApi;
import com.cloudera.smartrouter.client.api.DecisionsApi;
import com.cloudera.smartrouter.client.api.DefaultApi;
import com.cloudera.smartrouter.client.api.GuardrailsApi;
import com.cloudera.smartrouter.client.api.LookupsApi;
import com.cloudera.smartrouter.client.api.OrderApi;
import com.cloudera.smartrouter.client.api.PoliciesApi;
import com.cloudera.smartrouter.client.api.QueuesApi;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;

/**
 * Entry point to the Smart Router HTTP API of a cluster. Every returned API instance is bound to the given cluster and acts on behalf of the given user.
 */
@Component
public class SmartRouterServiceClient {

    @Inject
    @Qualifier("smartRouterClusterProxyProxyClientFactory")
    private ClusterProxyProxyClientFactory clusterProxyProxyClientFactory;

    @Inject
    private SmartRouterServiceConfig smartRouterServiceConfig;

    /**
     * Routing resolution, health, readiness and provider snapshot endpoints.
     */
    public DefaultApi routingApi(String clusterCrn, String userCrn) {
        return new DefaultApi(apiClient(clusterCrn, userCrn));
    }

    public PoliciesApi policiesApi(String clusterCrn, String userCrn) {
        return new PoliciesApi(apiClient(clusterCrn, userCrn));
    }

    public OrderApi orderApi(String clusterCrn, String userCrn) {
        return new OrderApi(apiClient(clusterCrn, userCrn));
    }

    public AuthoringApi authoringApi(String clusterCrn, String userCrn) {
        return new AuthoringApi(apiClient(clusterCrn, userCrn));
    }

    public LookupsApi lookupsApi(String clusterCrn, String userCrn) {
        return new LookupsApi(apiClient(clusterCrn, userCrn));
    }

    public GuardrailsApi guardrailsApi(String clusterCrn, String userCrn) {
        return new GuardrailsApi(apiClient(clusterCrn, userCrn));
    }

    public DecisionsApi decisionsApi(String clusterCrn, String userCrn) {
        return new DecisionsApi(apiClient(clusterCrn, userCrn));
    }

    public QueuesApi queuesApi(String clusterCrn, String userCrn) {
        return new QueuesApi(apiClient(clusterCrn, userCrn));
    }

    private SmartRouterApiClient apiClient(String clusterCrn, String userCrn) {
        ClusterProxyProxyClient clusterProxyProxyClient = clusterProxyProxyClientFactory.create(
                clusterCrn, smartRouterServiceConfig.getClusterProxyServiceName(), userCrn);
        return new SmartRouterApiClient(clusterProxyProxyClient);
    }
}
