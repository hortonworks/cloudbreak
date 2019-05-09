package com.sequenceiq.cloudbreak.cm.client;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
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
        if (StringUtils.isNoneBlank(cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword())) {
            return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig,
                    stack.getGatewayPort(), cluster.getCloudbreakAmbariUser(), cluster.getCloudbreakAmbariPassword());
        } else {
            return getDefaultClient(stack, clientConfig);
        }
    }

    public ApiClient getClient(Stack stack, String username, String password, HttpClientConfig clientConfig) {
        return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig, stack.getGatewayPort(), username, password);
    }

    public ClouderaManagerResourceApi getClouderaManagerResourceApi(ApiClient apiClient) {
        return new ClouderaManagerResourceApi(apiClient);
    }

    public MgmtServiceResourceApi getMgmtServiceResourceApi(ApiClient apiClient) {
        return new MgmtServiceResourceApi(apiClient);
    }

    public ExternalUserMappingsResourceApi getExternalUserMappingsResourceApi(ApiClient client) {
        return new ExternalUserMappingsResourceApi(client);
    }

    public AuthRolesResourceApi getAuthRolesResourceApi(ApiClient client) {
        return new AuthRolesResourceApi(client);
    }

    public ClustersResourceApi getClustersResourceApi(ApiClient apiClient) {
        return new ClustersResourceApi(apiClient);
    }

    public HostsResourceApi getHostsResourceApi(ApiClient client) {
        return new HostsResourceApi(client);
    }

    public ServicesResourceApi getServicesResourceApi(ApiClient client) {
        return new ServicesResourceApi(client);
    }

    public RolesResourceApi getRolesResourceApi(ApiClient client) {
        return new RolesResourceApi(client);
    }

    public HostTemplatesResourceApi getHostTemplatesResourceApi(ApiClient client) {
        return new HostTemplatesResourceApi(client);
    }

    public ParcelResourceApi getParcelResourceApi(ApiClient apiClient) {
        return new ParcelResourceApi(apiClient);
    }

    public CommandsResourceApi getCommandsResourceApi(ApiClient apiClient) {
        return new CommandsResourceApi(apiClient);
    }
}
