package com.sequenceiq.cloudbreak.cm.client.retry;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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

@Configuration
public class CmClientConfig {

    // factory functions:
    @Bean
    public Function<ApiClient, ClouderaManagerResourceApi> clouderaManagerResourceApiFactory() {
        return this::clouderaManagerResourceApi;
    }

    @Bean
    public Function<ApiClient, MgmtServiceResourceApi> mgmtServiceResourceApiFactory() {
        return this::mgmtServiceResourceApi;
    }

    @Bean
    public Function<ApiClient, ExternalUserMappingsResourceApi> externalUserMappingsResourceApiFactory() {
        return this::externalUserMappingsResourceApi;
    }

    @Bean
    public Function<ApiClient, AuthRolesResourceApi> authRolesResourceApiFactory() {
        return this::authRolesResourceApi;
    }

    @Bean
    public Function<ApiClient, ClustersResourceApi> clustersResourceApiFactory() {
        return this::clustersResourceApi;
    }

    @Bean
    public Function<ApiClient, HostsResourceApi> hostsResourceApiFactory() {
        return this::hostsResourceApi;
    }

    @Bean
    public Function<ApiClient, ServicesResourceApi> servicesResourceApiFactory() {
        return this::servicesResourceApi;
    }

    @Bean
    public Function<ApiClient, RolesResourceApi> rolesResourceApiFactory() {
        return this::rolesResourceApi;
    }

    @Bean
    public Function<ApiClient, RoleConfigGroupsResourceApi> roleConfigGroupsResourceApiFactory() {
        return this::roleConfigGroupsResourceApi;
    }

    @Bean
    public Function<ApiClient, RoleCommandsResourceApi> roleCommandsResourceApiFactory() {
        return this::roleCommandsResourceApi;
    }

    @Bean
    public Function<ApiClient, HostTemplatesResourceApi> hostTemplatesResourceApiFactory() {
        return this::hostTemplatesResourceApi;
    }

    @Bean
    public Function<ApiClient, ParcelResourceApi> parcelResourceApiFactory() {
        return this::parcelResourceApi;
    }

    @Bean
    public Function<ApiClient, ParcelsResourceApi> parcelsResourceApiFactory() {
        return this::parcelsResourceApi;
    }

    @Bean
    public Function<ApiClient, CommandsResourceApi> commandsResourceApiFactory() {
        return this::commandsResourceApi;
    }

    @Bean
    public Function<ApiClient, UsersResourceApi> usersResourceApiFactory() {
        return this::usersResourceApi;
    }

    @Bean
    public Function<ApiClient, CdpResourceApi> cdpResourceApiFactory() {
        return this::cdpResourceApi;
    }

    @Bean
    public Function<ApiClient, MgmtRolesResourceApi> mgmtRolesResourceApiFactory() {
        return this::mgmtRolesResourceApi;
    }

    @Bean
    public Function<ApiClient, MgmtRoleConfigGroupsResourceApi> mgmtRoleConfigGroupsResourceApiFactory() {
        return this::mgmtRoleConfigGroupsResourceApi;
    }

    @Bean
    public Function<ApiClient, AllHostsResourceApi> allHostsResourceApiFactory() {
        return this::allHostsResourceApi;
    }

    @Bean
    public Function<ApiClient, ToolsResourceApi> toolsResourceApiFactory() {
        return this::toolsResourceApi;
    }

    @Bean
    public Function<ApiClient, ExternalAccountsResourceApi> externalAccountsResourceApiFactory() {
        return this::externalAccountsResourceApi;
    }

    // prototype bean declarations:
    // CHECKSTYLE:OFF
    @Bean
    @Scope(value = "prototype")
    public ClouderaManagerResourceApi clouderaManagerResourceApi(ApiClient apiClient) {
        return new ClouderaManagerResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public MgmtServiceResourceApi mgmtServiceResourceApi(ApiClient apiClient) {
        return new MgmtServiceResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ExternalUserMappingsResourceApi externalUserMappingsResourceApi(ApiClient apiClient) {
        return new ExternalUserMappingsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public AuthRolesResourceApi authRolesResourceApi(ApiClient apiClient) {
        return new AuthRolesResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ClustersResourceApi clustersResourceApi(ApiClient apiClient) {
        return new ClustersResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public HostsResourceApi hostsResourceApi(ApiClient apiClient) {
        return new HostsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ServicesResourceApi servicesResourceApi(ApiClient apiClient) {
        return new ServicesResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public RolesResourceApi rolesResourceApi(ApiClient apiClient) {
        return new RolesResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public RoleConfigGroupsResourceApi roleConfigGroupsResourceApi(ApiClient apiClient) {
        return new RoleConfigGroupsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public RoleCommandsResourceApi roleCommandsResourceApi(ApiClient apiClient) {
        return new RoleCommandsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public HostTemplatesResourceApi hostTemplatesResourceApi(ApiClient apiClient) {
        return new HostTemplatesResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ParcelResourceApi parcelResourceApi(ApiClient apiClient) {
        return new ParcelResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ParcelsResourceApi parcelsResourceApi(ApiClient apiClient) {
        return new ParcelsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public CommandsResourceApi commandsResourceApi(ApiClient apiClient) {
        return new CommandsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public UsersResourceApi usersResourceApi(ApiClient apiClient) {
        return new UsersResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public CdpResourceApi cdpResourceApi(ApiClient apiClient) {
        return new CdpResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public MgmtRolesResourceApi mgmtRolesResourceApi(ApiClient apiClient) {
        return new MgmtRolesResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi(ApiClient apiClient) {
        return new MgmtRoleConfigGroupsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public AllHostsResourceApi allHostsResourceApi(ApiClient apiClient) {
        return new AllHostsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ToolsResourceApi toolsResourceApi(ApiClient apiClient) {
        return new ToolsResourceApi(apiClient);
    }

    @Bean
    @Scope(value = "prototype")
    public ExternalAccountsResourceApi externalAccountsResourceApi(ApiClient apiClient) {
        return new ExternalAccountsResourceApi(apiClient);
    }
    // CHECKSTYLE:ON
}
