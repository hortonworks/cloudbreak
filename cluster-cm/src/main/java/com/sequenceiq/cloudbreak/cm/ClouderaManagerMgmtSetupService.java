package com.sequenceiq.cloudbreak.cm;

import static java.util.Objects.requireNonNull;

import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.MgmtRolesResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cm.database.DatabaseProperties;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.util.DatabaseCommon;
import com.sequenceiq.cloudbreak.util.HostAndPortAndDatabaseName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sets up management services for the given Cloudera Manager server.
 */
@Service
public class ClouderaManagerMgmtSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerMgmtSetupService.class);

    private static final String MGMT_SERVICE = "MGMT";

    // This map contains the roles and their internal config name that's necessary for setting database config values
    private static final Map<String, String> ROLES_THAT_NEED_DATABASES = ImmutableMap.of(
            "ACTIVITYMANAGER", "firehose",
            "REPORTSMANAGER", "headlamp",
            "NAVIGATOR", "navigator",
            "NAVIGATORMETASERVER", "nav_metaserver");

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    /**
     * Sets up the management services using the given Cloudera Manager client.
     *
     * @param stack         the stack
     * @param client        the CM API client
     * @param mgmtRdsConfig the database configuration
     * @throws ApiException if there's a problem setting up management services
     */
    public void setupMgmtServices(Stack stack, ApiClient client, Optional<RDSConfig> mgmtRdsConfig) throws ApiException {
        MgmtServiceResourceApi mgmtServiceResourceApi = new MgmtServiceResourceApi(client);

        ApiService mgmtService = new ApiService();
        mgmtService.setName(MGMT_SERVICE);
        mgmtService.setType(MGMT_SERVICE);
        mgmtServiceResourceApi.setupCMS(mgmtService);

        // Better to auto-assign roles instead of specifying them explicitly in case TELEMETRYPUBLISHER is enabled
        mgmtServiceResourceApi.autoAssignRoles();

        // If we don't have a database configuration, we can still set up MGMT services. Some of them won't work,
        // but we'll still get things like host monitoring.
        if (mgmtRdsConfig.isPresent()) {
            createMgmtDatabases(client, mgmtRdsConfig.get());
        }

        clouderaManagerPollingServiceProvider.startManagementServicePollingService(stack, client,
                mgmtServiceResourceApi.startCommand().getId());
    }

    private void createMgmtDatabases(ApiClient client, RDSConfig mgmtRdsConfig) throws ApiException {
        MgmtRolesResourceApi mgmtRolesResourceApi = new MgmtRolesResourceApi(client);
        MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi = new MgmtRoleConfigGroupsResourceApi(client);

        List<ApiRole> installedRoles = mgmtRolesResourceApi.readRoles().getItems();

        Set<String> roleTypesThatNeedDatabases = ROLES_THAT_NEED_DATABASES.keySet();
        for (ApiRole role : installedRoles) {
            String roleType = role.getType();
            if (roleTypesThatNeedDatabases.contains(roleType)) {
                String internalRoleTypeName = ROLES_THAT_NEED_DATABASES.get(roleType);
                mgmtRoleConfigGroupsResourceApi.updateConfig(getBaseRoleConfigGroupName(roleType),
                        "Adding database settings",
                        buildApiConfigList(internalRoleTypeName, mgmtRdsConfig));
            }
        }
    }

    /**
     * Generate the default internal name of the mgmt RCGs as defined by CM.
     */
    @VisibleForTesting
    String getBaseRoleConfigGroupName(String configuration) {
        requireNonNull(configuration);
        return Joiner.on("-").join("MGMT", configuration, "BASE");
    }

    /**
     * Builds an API config list given the database configuration. The prefix is added onto
     * the beginning of the database config names to get the accurate database configuration key
     * to assign a value to.
     *
     * @param prefix    the prefix to use to generate database config keys
     * @param rdsConfig the database configuration
     * @return an API config list
     */
    @VisibleForTesting
    ApiConfigList buildApiConfigList(String prefix, RDSConfig rdsConfig) {
        String connectionUrl = rdsConfig.getConnectionURL();

        // We can safely retrieve these values since they should have passed validation
        String type = DatabaseCommon.getDatabaseType(connectionUrl).get();
        HostAndPortAndDatabaseName hostAndPortAndDatabaseName = DatabaseCommon.getHostPortAndDatabaseName(
                connectionUrl).get();

        ApiConfigList result = new ApiConfigList();
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.TYPE_PROPERTY),
                type));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.HOST_PROPERTY),
                hostAndPortAndDatabaseName.getHostAndPort()));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.NAME_PROPERTY),
                hostAndPortAndDatabaseName.getDatabaseName()));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.USER_PROPERTY),
                rdsConfig.getConnectionUserName()));
        result.addItemsItem(makeApiConfig(getDatabaseApiConfigName(prefix, DatabaseProperties.PASSWORD_PROPERTY),
                rdsConfig.getConnectionPassword()));

        return result;
    }

    private String getDatabaseApiConfigName(String prefix, String propertyName) {
        return String.format("%s_database_%s", prefix, propertyName);
    }

    private ApiConfig makeApiConfig(String name, String value) {
        ApiConfig apiConfig = new ApiConfig();

        apiConfig.setName(name);
        apiConfig.setValue(value);

        return apiConfig;
    }
}
