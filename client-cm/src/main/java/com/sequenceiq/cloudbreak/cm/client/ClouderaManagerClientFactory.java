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
import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;

@Service
public class ClouderaManagerClientFactory {

    @Inject
    private ClouderaManagerClientProvider clouderaManagerClientProvider;

    public ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig, gatewayPort, "admin", "admin");
    }

    public ApiClient getClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return clouderaManagerClientProvider.getClouderaManagerClient(clientConfig,
                    gatewayPort, user, password);
        } else {
            return getDefaultClient(gatewayPort, clientConfig);
        }
    }

    public ApiClient getRootClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return clouderaManagerClientProvider.getClouderaManagerRootClient(clientConfig,
                    gatewayPort, user, password);
        } else {
            return clouderaManagerClientProvider.getClouderaManagerRootClient(clientConfig, gatewayPort, "admin", "admin");
        }
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

    public RoleCommandsResourceApi getRoleCommandsResourceApi(ApiClient client) {
        return new RoleCommandsResourceApi(client);
    }

    public HostTemplatesResourceApi getHostTemplatesResourceApi(ApiClient client) {
        return new HostTemplatesResourceApi(client);
    }

    public ParcelResourceApi getParcelResourceApi(ApiClient apiClient) {
        return new ParcelResourceApi(apiClient);
    }

    public ParcelsResourceApi getParcelsResourceApi(ApiClient apiClient) {
        return new ParcelsResourceApi(apiClient);
    }

    public CommandsResourceApi getCommandsResourceApi(ApiClient apiClient) {
        return new CommandsResourceApi(apiClient);
    }
}
