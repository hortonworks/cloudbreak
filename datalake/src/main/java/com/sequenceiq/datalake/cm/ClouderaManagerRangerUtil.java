package com.sequenceiq.datalake.cm;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBulkCommandList;
import com.cloudera.api.swagger.model.ApiClusterList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleNameList;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;

import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ClouderaManagerRangerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRangerUtil.class);

    private static final String RANGER_SERVICE_NAME = "ranger";

    private static final String RANGER_USER_SYNC_ROLE_TYPE = "RANGER_USERSYNC";

    private static final String AZURE_USER_MAPPING = "ranger_usersync_azure_user_mapping";

    private static final MapJoiner CLOUD_IDENTITY_CONFIG_MAP_JOINER =
            Joiner.on(";").withKeyValueSeparator("=");

    @Inject
    private ClouderaManagerProxiedClientFactory clouderaManagerProxiedClientFactory;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private String getClusterName(ApiClient client) throws ApiException {
        ClustersResourceApi clustersResource = clouderaManagerApiFactory.getClustersResourceApi(client);
        ApiClusterList clusterList = clustersResource.readClusters(null, null);
        return Iterables.getOnlyElement(clusterList.getItems()).getName();
    }

    private String getRangerUserSyncRoleName(ApiClient client, String clusterName) throws ApiException {
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        List<ApiRole> apiRoleList =  rolesResourceApi.readRoles(clusterName, RANGER_SERVICE_NAME, null, null)
                .getItems()
                .stream()
                .filter(apiRole -> RANGER_USER_SYNC_ROLE_TYPE.equals(apiRole.getType()))
                .collect(Collectors.toList());
        return Iterables.getOnlyElement(apiRoleList).getName();
    }

    private ApiConfig newCloudIdentityConfig(String configName, Map<String, String> configValues) {
        // NOTE: We sort the configs first. This isn't really necessary but is more consistent.
        ImmutableSortedMap<String, String> configValuesSorted = ImmutableSortedMap.copyOf(configValues);
        ApiConfig config = new ApiConfig();
        config.setName(configName);
        config.setValue(CLOUD_IDENTITY_CONFIG_MAP_JOINER.join(configValuesSorted));
        return config;
    }

    private ApiCommand triggerRoleRefresh(ApiClient client, String clusterName, String serviceName, String roleName) throws ApiException {
        LOGGER.info("Attempting to trigger role refresh on clusterName = {}, serviceName = {}, roleName = {}", clusterName, serviceName, roleName);
        ApiRoleNameList roleNameList = new ApiRoleNameList();
        roleNameList.addItemsItem(roleName);
        RoleCommandsResourceApi roleCommandsResourceApi = clouderaManagerApiFactory.getRoleCommandsResourceApi(client);
        ApiBulkCommandList bulkResponse = roleCommandsResourceApi.refreshCommand(clusterName, serviceName, roleNameList);
        return Iterables.getOnlyElement(bulkResponse.getItems());
    }

    private boolean isCloudIdMappingSupported(RolesResourceApi rolesResourceApi, String clusterName, String rangerUserSyncRole) throws ApiException {
        ApiConfigList configList = rolesResourceApi.readRoleConfig(clusterName, rangerUserSyncRole, RANGER_SERVICE_NAME, "full");
        return configList.getItems().stream().map(ApiConfig::getName).anyMatch(configName -> configName.equals(AZURE_USER_MAPPING));
    }

    private boolean isRoleRefreshNeeded(RolesResourceApi rolesResourceApi, String clusterName, String rangerUserSyncRole) throws ApiException {
        ApiRole role = rolesResourceApi.readRole(clusterName, rangerUserSyncRole, RANGER_SERVICE_NAME, "summary");
        ApiConfigStalenessStatus stalenessStatus = role.getConfigStalenessStatus();
        LOGGER.debug("Ranger user sync ApiConfigStalenessStatus = {}", stalenessStatus);
        return stalenessStatus.equals(ApiConfigStalenessStatus.STALE_REFRESHABLE);
    }

    private Optional<ApiCommand> refreshRoleIfStale(ApiClient client, RolesResourceApi rolesResourceApi, String clusterName, String rangerUserSyncRoleName)
            throws ApiException {
        if (isRoleRefreshNeeded(rolesResourceApi, clusterName, rangerUserSyncRoleName)) {
            LOGGER.info("Role refresh required, trigerring role refresh");
            ApiCommand command = triggerRoleRefresh(client, clusterName, RANGER_SERVICE_NAME, rangerUserSyncRoleName);
            return Optional.of(command);
        } else {
            LOGGER.info("No role refresh required");
            return Optional.empty();
        }
    }

    public boolean isCloudIdMappingSupported(String stackCrn) throws ApiException {
        // NOTE: The necessary configs changed here are only available in CM7.2-1
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        String clusterName = getClusterName(client);
        String rangerUserSyncRoleName = getRangerUserSyncRoleName(client, clusterName);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        return isCloudIdMappingSupported(rolesResourceApi, clusterName, rangerUserSyncRoleName);
    }

    public Optional<ApiCommand> setAzureCloudIdentityMapping(String stackCrn, Map<String, String> azureUserMapping) throws ApiException {
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        String clusterName = getClusterName(client);
        String rangerUserSyncRoleName = getRangerUserSyncRoleName(client, clusterName);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        ApiConfigList configList = new ApiConfigList();
        configList.addItemsItem(newCloudIdentityConfig(AZURE_USER_MAPPING, azureUserMapping));
        rolesResourceApi.updateRoleConfig(clusterName, rangerUserSyncRoleName, RANGER_SERVICE_NAME,
                "Updating Azure Cloud Identity Mapping through Cloudbreak",
                configList);
        return refreshRoleIfStale(client, rolesResourceApi, clusterName, rangerUserSyncRoleName);
    }

    public ApiCommand getApiCommand(String stackCrn, long commandId) throws ApiException {
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        return commandsResourceApi.readCommand(BigDecimal.valueOf(commandId));
    }

}
