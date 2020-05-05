package com.sequenceiq.cloudbreak.cm.client.retry;

import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.AllHostsResourceApi;
import com.cloudera.api.swagger.AuthRolesResourceApi;
import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.ExternalAccountsResourceApi;
import com.cloudera.api.swagger.ExternalUserMappingsResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.MgmtRolesResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.ToolsResourceApi;
import com.cloudera.api.swagger.UsersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;

@Component
public class ClouderaManagerApiFactory {

    @Inject
    private Function<ApiClient, ClouderaManagerResourceApi> clouderaManagerResourceApiFactory;

    @Inject
    private Function<ApiClient, MgmtServiceResourceApi> mgmtServiceResourceApiFactory;

    @Inject
    private Function<ApiClient, ExternalUserMappingsResourceApi> externalUserMappingsResourceApiFactory;

    @Inject
    private Function<ApiClient, AuthRolesResourceApi> authRolesResourceApiFactory;

    @Inject
    private Function<ApiClient, ClustersResourceApi> clustersResourceApiFactory;

    @Inject
    private Function<ApiClient, HostsResourceApi> hostsResourceApiFactory;

    @Inject
    private Function<ApiClient, ServicesResourceApi> servicesResourceApiFactory;

    @Inject
    private Function<ApiClient, RolesResourceApi> rolesResourceApiFactory;

    @Inject
    private Function<ApiClient, RoleConfigGroupsResourceApi> roleConfigGroupsResourceApiFactory;

    @Inject
    private Function<ApiClient, RoleCommandsResourceApi> roleCommandsResourceApiFactory;

    @Inject
    private Function<ApiClient, HostTemplatesResourceApi> hostTemplatesResourceApiFactory;

    @Inject
    private Function<ApiClient, ParcelResourceApi> parcelResourceApiFactory;

    @Inject
    private Function<ApiClient, ParcelsResourceApi> parcelsResourceApiFactory;

    @Inject
    private Function<ApiClient, CommandsResourceApi> commandsResourceApiFactory;

    @Inject
    private Function<ApiClient, UsersResourceApi> usersResourceApiFactory;

    @Inject
    private Function<ApiClient, CdpResourceApi> cdpResourceApiFactory;

    @Inject
    private Function<ApiClient, MgmtRolesResourceApi> mgmtRolesResourceApiFactory;

    @Inject
    private Function<ApiClient, MgmtRoleConfigGroupsResourceApi> mgmtRoleConfigGroupsResourceApiFactory;

    @Inject
    private Function<ApiClient, AllHostsResourceApi> allHostsResourceApiFactory;

    @Inject
    private Function<ApiClient, ToolsResourceApi> toolsResourceApiFactory;

    @Inject
    private Function<ApiClient, ExternalAccountsResourceApi> externalAccountsResourceApiFactory;

    public ClouderaManagerResourceApi getClouderaManagerResourceApi(ApiClient apiClient) {
        return clouderaManagerResourceApiFactory.apply(apiClient);
    }

    public MgmtServiceResourceApi getMgmtServiceResourceApi(ApiClient apiClient) {
        return mgmtServiceResourceApiFactory.apply(apiClient);
    }

    public ExternalUserMappingsResourceApi getExternalUserMappingsResourceApi(ApiClient apiClient) {
        return externalUserMappingsResourceApiFactory.apply(apiClient);
    }

    public AuthRolesResourceApi getAuthRolesResourceApi(ApiClient apiClient) {
        return authRolesResourceApiFactory.apply(apiClient);
    }

    public ClustersResourceApi getClustersResourceApi(ApiClient apiClient) {
        return clustersResourceApiFactory.apply(apiClient);
    }

    public HostsResourceApi getHostsResourceApi(ApiClient apiClient) {
        return hostsResourceApiFactory.apply(apiClient);
    }

    public ServicesResourceApi getServicesResourceApi(ApiClient apiClient) {
        return servicesResourceApiFactory.apply(apiClient);
    }

    public RolesResourceApi getRolesResourceApi(ApiClient apiClient) {
        return rolesResourceApiFactory.apply(apiClient);
    }

    public RoleConfigGroupsResourceApi getRoleConfigGroupsResourceApi(ApiClient apiClient) {
        return roleConfigGroupsResourceApiFactory.apply(apiClient);
    }

    public RoleCommandsResourceApi getRoleCommandsResourceApi(ApiClient apiClient) {
        return roleCommandsResourceApiFactory.apply(apiClient);
    }

    public HostTemplatesResourceApi getHostTemplatesResourceApi(ApiClient apiClient) {
        return hostTemplatesResourceApiFactory.apply(apiClient);
    }

    public ParcelResourceApi getParcelResourceApi(ApiClient apiClient) {
        return parcelResourceApiFactory.apply(apiClient);
    }

    public ParcelsResourceApi getParcelsResourceApi(ApiClient apiClient) {
        return parcelsResourceApiFactory.apply(apiClient);
    }

    public CommandsResourceApi getCommandsResourceApi(ApiClient apiClient) {
        return commandsResourceApiFactory.apply(apiClient);
    }

    public UsersResourceApi getUserResourceApi(ApiClient apiClient) {
        return usersResourceApiFactory.apply(apiClient);
    }

    public CdpResourceApi getCdpResourceApi(ApiClient apiClient) {
        return cdpResourceApiFactory.apply(apiClient);
    }

    public MgmtRolesResourceApi getMgmtRolesResourceApi(ApiClient apiClient) {
        return mgmtRolesResourceApiFactory.apply(apiClient);
    }

    public MgmtRoleConfigGroupsResourceApi getMgmtRoleConfigGroupsResourceApi(ApiClient apiClient) {
        return mgmtRoleConfigGroupsResourceApiFactory.apply(apiClient);
    }

    public AllHostsResourceApi getAllHostsResourceApi(ApiClient apiClient) {
        return allHostsResourceApiFactory.apply(apiClient);
    }

    public ToolsResourceApi getToolsResourceApi(ApiClient client) {
        return toolsResourceApiFactory.apply(client);
    }

    public ExternalAccountsResourceApi getExternalAccountsResourceApi(ApiClient client) {
        return externalAccountsResourceApiFactory.apply(client);
    }
}
