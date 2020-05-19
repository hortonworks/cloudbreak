package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiClusterList;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ClouderaManagerRangerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRangerUtil.class);

    private static final String RANGER_SERVICE_NAME = "ranger";

    private static final String RANGER_USER_SYNC_ROLE_TYPE = "RANGER_USERSYNC";

    private static final String AZURE_USER_MAPPING = "ranger_usersync_azure_user_mapping";

    private static final String AZURE_GROUP_MAPPING = "ranger_usersync_azure_group_mapping";

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
                .filter(apiRole -> apiRole.getType().equals(RANGER_USER_SYNC_ROLE_TYPE))
                .collect(Collectors.toList());
        return Iterables.getOnlyElement(apiRoleList).getName();
    }

    private ApiConfig newCloudIdentityConfig(String configName, Map<String, String> configValues) {
        ApiConfig config = new ApiConfig();
        config.setName(configName);
        config.setValue(CLOUD_IDENTITY_CONFIG_MAP_JOINER.join(configValues));
        return config;
    }

    public void updateAzureCloudIdentityMapping(String clouderaManagerStackCrn,
                                                Map<String, String> azureUserMapping,
                                                Map<String, String> azureGroupMapping) throws ApiException {
        // NOTE: The necessary configs changed here are only available in CM7.2-1
        ApiClient client = clouderaManagerProxiedClientFactory.getProxiedClouderaManagerClient(clouderaManagerStackCrn);
        String clusterName = getClusterName(client);
        String rangerUserSyncRoleName = getRangerUserSyncRoleName(client, clusterName);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        ApiConfigList configList = new ApiConfigList();
        configList.addItemsItem(newCloudIdentityConfig(AZURE_USER_MAPPING, azureUserMapping));
        configList.addItemsItem(newCloudIdentityConfig(AZURE_GROUP_MAPPING, azureGroupMapping));
        rolesResourceApi.updateRoleConfig(clusterName, rangerUserSyncRoleName, RANGER_SERVICE_NAME,
                "Updating Azure Cloud Identity Mapping through Cloudbreak",
                configList);
        // TODO : The ranger role needs to be refreshed
    }
}
