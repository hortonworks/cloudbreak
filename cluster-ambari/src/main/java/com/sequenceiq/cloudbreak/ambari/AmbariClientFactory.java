package com.sequenceiq.cloudbreak.ambari;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class AmbariClientFactory {

    @Inject
    private AmbariClientProvider ambariClientProvider;

    public AmbariClient getDefaultAmbariClient(Stack stack, HttpClientConfig clientConfig) {
        return ambariClientProvider.getDefaultAmbariClient(clientConfig, stack.getGatewayPort());
    }

    public AmbariClient getAmbariClient(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), cluster);
    }

    public AmbariClient getAmbariClient(Stack stack, String username, String password, HttpClientConfig clientConfig) {
        return ambariClientProvider.getAmbariClient(clientConfig, stack.getGatewayPort(), username, password);
    }
}
