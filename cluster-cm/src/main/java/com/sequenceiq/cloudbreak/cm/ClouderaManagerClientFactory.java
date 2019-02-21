package com.sequenceiq.cloudbreak.cm;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class ClouderaManagerClientFactory {

    @Inject
    private ClouderaManagerClientProvider clouderaManagerClientProvider;

    public ApiClient getDefaultClient(Stack stack, HttpClientConfig clientConfig) {
        return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig, stack.getGatewayPort(), "admin", "admin");
    }

    public ApiClient getClient(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig,
                stack.getGatewayPort(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword());
    }

    public ApiClient getClient(Stack stack, String username, String password, HttpClientConfig clientConfig) {
        return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig, stack.getGatewayPort(), username, password);
    }
}
