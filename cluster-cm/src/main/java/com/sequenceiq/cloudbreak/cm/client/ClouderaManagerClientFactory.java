package com.sequenceiq.cloudbreak.cm.client;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
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

    public ClouderaManagerResourceApi getClouderaManagerResourceApi(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return getClouderaManagerResourceApi(getClient(stack, cluster, clientConfig));
    }

    public ClouderaManagerResourceApi getClouderaManagerResourceApi(ApiClient apiClient) {
        return new ClouderaManagerResourceApi(apiClient);
    }

    public ExternalUserMappingsResourceApi getExternalUserMappingsResourceApi(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return new ExternalUserMappingsResourceApi(getClient(stack, cluster, clientConfig));
    }

    public AuthRolesResourceApi getAuthRolesResourceApi(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return new AuthRolesResourceApi(getClient(stack, cluster, clientConfig));
    }

    public ClustersResourceApi getClustersResourceApi(Stack stack, Cluster cluster, HttpClientConfig clientConfig) {
        return getClustersResourceApi(getClient(stack, cluster, clientConfig));
    }

    public ClustersResourceApi getClustersResourceApi(ApiClient apiClient) {
        return new ClustersResourceApi(apiClient);
    }
}
