package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.template.views.provider.RdsViewProvider;
import com.sequenceiq.cloudbreak.validation.AllRoleTypes;

@Service
public class ClusterServicesRestartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartService.class);

    private static final String HIVE_SERVICE = "HIVE";

    private static final String HIVE_METASTORE_DATABASE_HOST = "hive_metastore_database_host";

    private static final String HIVE_METASTORE_DATABASE_NAME = "hive_metastore_database_name";

    private static final String HIVE_METASTORE_DATABASE_PASSWORD = "hive_metastore_database_password";

    private static final String HIVE_METASTORE_DATABASE_PORT = "hive_metastore_database_port";

    private static final String HIVE_METASTORE_DATABASE_TYPE = "hive_metastore_database_type";

    private static final String HIVE_METASTORE_DATABASE_USER = "hive_metastore_database_user";

    private static final String JDBC_URL_OVERRIDE = "jdbc_url_override";

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private RdsViewProvider rdsViewProvider;

    public boolean isRemoteDataContextRefreshNeeded(Stack stack, Stack datalakeStack) {
        // Re-configuring DH using the Remote Data Context of Data lake.
        boolean clusterDataHub = stack.getType().equals(StackType.WORKLOAD);
        boolean dlIsRebuild = isDatalakeCreatedAfterDataHub(datalakeStack, stack);
        boolean resizeEntitlementEnabled = entitlementService.isDatalakeLightToMediumMigrationEnabled(ThreadBasedUserCrnProvider.getAccountId());
        LOGGER.info("Is cluster DH: {}, Is DL rebuild: {},  Is resize entitlement Enabled: {}",
                clusterDataHub,
                dlIsRebuild,
                resizeEntitlementEnabled);
        return clusterDataHub &&
                dlIsRebuild &&
                resizeEntitlementEnabled;
    }

    public void refreshClusterOnStart(Stack stack, Stack datalakeStack, CmTemplateProcessor blueprintProcessor) throws CloudbreakException {
        LOGGER.info("Triggering update of remote data context");
        apiConnectors.getConnector(stack).startClusterManagerAndAgents();
        refreshClusterOnRestart(stack, datalakeStack, blueprintProcessor);
    }

    public void refreshClusterOnRestart(Stack stack, Stack datalakeStack, CmTemplateProcessor blueprintProcessor) throws CloudbreakException {
        LOGGER.info("Triggering update of remote data context");
        clusterBuilderService.configureManagementServices(stack.getId());
        if (shouldReloadDatabaseConfig(blueprintProcessor)) {
            //Update Hive service database configuration
            LOGGER.info("Trying to refreshing the database configuration.");
            updateDatabaseConfiguration(datalakeStack, stack, HIVE_SERVICE, DatabaseType.HIVE);
        } else {
            LOGGER.info("Database configuration is not refreshed");
        }
        apiConnectors.getConnector(stack).startClusterServices();
    }

    private void updateDatabaseConfiguration(Stack datalakeStack, Stack dataHubStack, String service, DatabaseType databaseType) {
        Cluster cluster = clusterService.getByIdWithLists(datalakeStack.getCluster().getId());
        String databaseTypeString = databaseType.toString();
        Optional<RdsConfigWithoutCluster> rdsConfig = postgresConfigService.createRdsConfigIfNeeded(datalakeStack, cluster, databaseType)
                .stream().filter(config -> config.getType().equalsIgnoreCase(databaseTypeString))
                .findFirst();
        try {
            if (rdsConfig.isPresent()) {
                LOGGER.info("Refreshing the database configuration.");
                apiConnectors.getConnector(dataHubStack).updateServiceConfig(service, getRdsConfigMap(rdsConfig.get(), dataHubStack.getCloudPlatform()));
            } else {
                LOGGER.error("Could not find RDS configuration for Hive");
            }
        } catch (CloudbreakException e) {
            LOGGER.info("Exception while updating the Data-Hub configuration", e);
        }
    }

    private Map<String, String> getRdsConfigMap(RdsConfigWithoutCluster rdsConfig, String cloudPlatform) {
        RdsView hiveRdsView = rdsViewProvider.getRdsView(rdsConfig, databaseSslService.getSslCertsFilePath(), cloudPlatform);
        Map<String, String> configs = new HashMap<>();
        configs.put(HIVE_METASTORE_DATABASE_HOST, hiveRdsView.getHost());
        configs.put(HIVE_METASTORE_DATABASE_NAME, hiveRdsView.getDatabaseName());
        configs.put(HIVE_METASTORE_DATABASE_PASSWORD, hiveRdsView.getConnectionPassword());
        configs.put(HIVE_METASTORE_DATABASE_PORT, hiveRdsView.getPort());
        configs.put(HIVE_METASTORE_DATABASE_TYPE, hiveRdsView.getSubprotocol());
        configs.put(HIVE_METASTORE_DATABASE_USER, hiveRdsView.getConnectionUserName());
        configs.put(JDBC_URL_OVERRIDE, hiveRdsView.getConnectionURL());
        return configs;
    }

    private boolean isDatalakeCreatedAfterDataHub(Stack datalakeStack, Stack dataHubStack) {
        return datalakeStack.getCreated() > dataHubStack.getCreated();
    }

    private boolean shouldReloadDatabaseConfig(CmTemplateProcessor blueprintProcessor) {
        return blueprintProcessor.doesCMComponentExistsInBlueprint(AllRoleTypes.HIVEMETASTORE.name());
    }
}
