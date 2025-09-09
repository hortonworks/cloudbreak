package com.sequenceiq.datalake.cm;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

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

    private List<String> getRangerUserSyncRoleName(ApiClient client, String clusterName) throws ApiException {
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        return rolesResourceApi.readRoles(clusterName, RANGER_SERVICE_NAME, null, null)
                .getItems()
                .stream()
                .filter(apiRole -> RANGER_USER_SYNC_ROLE_TYPE.equals(apiRole.getType()))
                .map(ApiRole::getName)
                .collect(Collectors.toList());
    }

    private ApiConfig newCloudIdentityConfig(String configName, Map<String, String> configValues) {
        // NOTE: We sort the configs first. This isn't really necessary but is more consistent.
        ImmutableSortedMap<String, String> configValuesSorted = ImmutableSortedMap.copyOf(configValues);
        ApiConfig config = new ApiConfig();
        config.setName(configName);
        config.setValue(CLOUD_IDENTITY_CONFIG_MAP_JOINER.join(configValuesSorted));
        return config;
    }

    private List<ApiCommand> triggerRoleRefresh(ApiClient client, String clusterName, String serviceName, List<String> roleNames) throws ApiException {
        LOGGER.info("Triggering role refresh on clusterName = {}, serviceName = {}, roleNames = {}", clusterName, serviceName, roleNames);
        ApiRoleNameList roleNameList = new ApiRoleNameList();
        roleNameList.items(roleNames);
        RoleCommandsResourceApi roleCommandsResourceApi = clouderaManagerApiFactory.getRoleCommandsResourceApi(client);
        ApiBulkCommandList bulkResponse = roleCommandsResourceApi.refreshCommand(clusterName, serviceName, roleNameList);
        return bulkResponse.getItems();
    }

    private boolean isCloudIdMappingSupported(RolesResourceApi rolesResourceApi, String clusterName, String rangerUserSyncRole) throws ApiException {
        ApiConfigList configList = rolesResourceApi.readRoleConfig(clusterName, rangerUserSyncRole, RANGER_SERVICE_NAME, "full");
        return configList.getItems().stream().map(ApiConfig::getName).anyMatch(configName -> configName.equals(AZURE_USER_MAPPING));
    }

    private boolean isRoleStale(RolesResourceApi rolesResourceApi, String clusterName, List<String> rangerUserSyncRoles) {
        return rangerUserSyncRoles.stream().anyMatch(rangerUserSyncRole -> {
            try {
                ApiRole role = rolesResourceApi.readRole(clusterName, rangerUserSyncRole, RANGER_SERVICE_NAME, "summary");
                ApiConfigStalenessStatus stalenessStatus = role.getConfigStalenessStatus();
                LOGGER.debug("Ranger user sync ApiConfigStalenessStatus = {}", stalenessStatus);
                return stalenessStatus.equals(ApiConfigStalenessStatus.STALE_REFRESHABLE);
            } catch (ApiException e) {
                throw new CloudbreakServiceException(e);
            }
        });
    }

    public boolean isCloudIdMappingSupported(String stackCrn) throws ApiException {
        // NOTE: The necessary configs changed here are only available in CM7.2-1
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        String clusterName = getClusterName(client);
        List<String> rangerUserSyncRoleNames = getRangerUserSyncRoleName(client, clusterName);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        return rangerUserSyncRoleNames.stream()
                .allMatch(rangerUserSyncRoleName -> {
                    try {
                        return isCloudIdMappingSupported(rolesResourceApi, clusterName, rangerUserSyncRoleName);
                    } catch (ApiException e) {
                        throw new CloudbreakServiceException(e);
                    }
                });
    }

    private Map<String, String> userMappingStrToMap(String userMappingStr) {
        Map<String, String> userMappingMap = new HashMap<>();

        userMappingStr = userMappingStr.trim();
        if (userMappingStr.isEmpty()) {
            LOGGER.info("Existing azure cloud mappings are empty");
        } else {
            String[] mappings = userMappingStr.split(";");
            Arrays.stream(mappings).forEach(mapping -> {
                String[] entry = mapping.split("=");
                if (entry.length == 2) {
                    String key = entry[0];
                    String val = entry[1];
                    userMappingMap.put(key, val);
                } else {
                    LOGGER.warn("Skipping malformed azure cloud mapping entry: {}", mapping);
                }
            });
        }
        return userMappingMap;
    }

    private Map<String, String> getExistingAzureUserMapping(RolesResourceApi rolesResourceApi, String clusterName, String rangerUserSyncRoleName)
            throws ApiException {
        ApiConfigList roleConfigList = rolesResourceApi.readRoleConfig(clusterName, rangerUserSyncRoleName, RANGER_SERVICE_NAME, "summary");
        Optional<ApiConfig> azureUserMappingConfig = roleConfigList.getItems().stream()
                .filter(apiConfig -> apiConfig.getName().equals(AZURE_USER_MAPPING))
                .findFirst();

        Map<String, String> userMappingMap;
        if (azureUserMappingConfig.isPresent()) {
            String azureUserMappingStr = azureUserMappingConfig.get().getValue();
            userMappingMap = userMappingStrToMap(azureUserMappingStr);
        } else {
            LOGGER.info("No azure cloud mapping config set.");
            userMappingMap = new HashMap<>();
        }

        return userMappingMap;
    }

    public List<ApiCommand> setAzureCloudIdentityMapping(String stackCrn, Map<String, String> requestedAzureUserMapping) throws ApiException {
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        String clusterName = getClusterName(client);
        List<String> rangerUserSyncRoleNames = getRangerUserSyncRoleName(client, clusterName);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);

        boolean existingMappingSameAsRequested = rangerUserSyncRoleNames.stream().allMatch(rangerUserSyncRoleName -> {
            try {
                Map<String, String> existingMappingStr = getExistingAzureUserMapping(rolesResourceApi, clusterName, rangerUserSyncRoleName);
                return existingMappingStr.equals(requestedAzureUserMapping);
            } catch (ApiException e) {
                throw new CloudbreakServiceException(e);
            }
        });

        // We only want to go through with the operation when ONE of the following conditions are met
        // 1) When the requested azure user mapping is different than the existing one
        // 2) When the role is stale. This is for the rare case when we previously were able to set the role and
        //    failed before trigerring refresh. This ensures that we trigger refresh in the subsequent call.
        boolean operationRequired = !existingMappingSameAsRequested ||
                isRoleStale(rolesResourceApi, clusterName, rangerUserSyncRoleNames);

        if (!operationRequired) {
            LOGGER.info("Existing azure cloud mappings are the same, nothing to do");
            return List.of();
        } else {
            LOGGER.info("Existing azure cloud mappings are different (or role is stale), setting mapping and triggering role refresh");
            ApiConfigList configList = new ApiConfigList();
            configList.addItemsItem(newCloudIdentityConfig(AZURE_USER_MAPPING, requestedAzureUserMapping));
            rangerUserSyncRoleNames.forEach(rangerUserSyncRoleName -> {
                try {
                    rolesResourceApi.updateRoleConfig(clusterName, rangerUserSyncRoleName, RANGER_SERVICE_NAME,
                            configList, "Updating Azure Cloud Identity Mapping through Cloudbreak");
                } catch (ApiException e) {
                    throw new CloudbreakServiceException(e);
                }
            });
            return triggerRoleRefresh(client, clusterName, RANGER_SERVICE_NAME, rangerUserSyncRoleNames);
        }
    }

    public ApiCommand getApiCommand(String stackCrn, long commandId) throws ApiException {
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(stackCrn);
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        return commandsResourceApi.readCommand(BigDecimal.valueOf(commandId));
    }

}
