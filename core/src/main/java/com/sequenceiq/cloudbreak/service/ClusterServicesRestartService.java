package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_TYPE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.AllRoleTypes;

@Service
public class ClusterServicesRestartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartService.class);

    private static final String HIVE_SERVICE = "HIVE";

    private static final String JDBC_URL_OVERRIDE = "jdbc_url_override";

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private StackService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterService clusterService;

    public boolean isRemoteDataContextRefreshNeeded(Stack stack, SdxBasicView sdxBasicView) {
        // Re-configuring DH using the Remote Data Context of Data lake.
        boolean clusterDataHub = stack.getType().equals(StackType.WORKLOAD);
        boolean dlIsRebuild = isDatalakeCreatedAfterDataHub(sdxBasicView, stack);
        boolean resizeEntitlementEnabled = entitlementService.isDatalakeLightToMediumMigrationEnabled(Crn.fromString(stack.getResourceCrn()).getAccountId());
        LOGGER.info("Is cluster DH: {}, Is DL rebuild: {},  Is resize entitlement Enabled: {}",
                clusterDataHub,
                dlIsRebuild,
                resizeEntitlementEnabled);
        return clusterDataHub &&
                dlIsRebuild &&
                resizeEntitlementEnabled;
    }

    public void refreshClusterOnStart(Stack stack, SdxBasicView sdxBasicView, CmTemplateProcessor blueprintProcessor) throws CloudbreakException {
        LOGGER.info("Triggering update of remote data context");
        apiConnectors.getConnector(stack).startClusterManagerAndAgents();
        refreshClusterOnRestart(stack, sdxBasicView, blueprintProcessor, false);
    }

    public void refreshClusterOnRestart(Stack stack, SdxBasicView sdxBasicView, CmTemplateProcessor blueprintProcessor, boolean rollingRestart)
            throws CloudbreakException {
        LOGGER.info("Triggering update of remote data context for {}", stack.getResourceCrn());
        clusterBuilderService.configureManagementServices(stack.getId());
        if (shouldReloadDatabaseConfig(blueprintProcessor)) {
            //Update Hive service database configuration
            LOGGER.info("Trying to refreshing the database configuration.");
            updateDatabaseConfiguration(sdxBasicView, stack, HIVE_SERVICE);
            updateHmsRdsConfig(stack, sdxBasicView);
        } else {
            LOGGER.info("Database configuration is not refreshed");
        }
        apiConnectors.getConnector(stack).restartClusterServices(rollingRestart);
    }

    private void updateHmsRdsConfig(Stack stack, SdxBasicView sdxBasicView) {
        Stack dlStack = stackService.getByCrn(sdxBasicView.crn());
        Set<RDSConfig> dlRdsConfigs = rdsConfigService.findByClusterId(dlStack.getClusterId());
        Set<RDSConfig> dhRdsConfigs = rdsConfigService.findByClusterId(stack.getClusterId());

        Optional<RDSConfig> dhRdsConfigOp =
                dhRdsConfigs
                        .stream()
                        .filter(config -> DatabaseType.HIVE.name().equals(config.getType()))
                        .findFirst();

        Optional<RDSConfig> dlRdsConfigOp =
                dlRdsConfigs
                        .stream()
                        .filter(config -> DatabaseType.HIVE.name().equals(config.getType()))
                        .findFirst();

        if (dhRdsConfigOp.isPresent() && dlRdsConfigOp.isPresent()) {
            RDSConfig dhRdsConfig = dhRdsConfigOp.get();
            RDSConfig dlRdsConfig = dlRdsConfigOp.get();
            LOGGER.info("Datahub HmsRdsConfig {} and Datalake HmsRdsConfig {} are not the same", dhRdsConfig.getId(), dlRdsConfig.getId());

            if (!Objects.equals(dhRdsConfig.getId(), dlRdsConfig.getId())) {
                dhRdsConfigs.remove(dhRdsConfig);
                dhRdsConfigs.add(dlRdsConfig);
                stack.getCluster().setRdsConfigs(dhRdsConfigs);
                clusterService.save(stack.getCluster());
                LOGGER.info("Datahub HmsRdsConfig updated to use {}", dhRdsConfig.getId());
            } else {
                LOGGER.info("Datahub HmsRdsConfig is already update. HmsRdsConfig id {}", dhRdsConfig.getId());
            }
        }
    }

    private void updateDatabaseConfiguration(SdxBasicView sdxBasicView, Stack dataHubStack, String service) {
        Map<String, String> hmsServiceConfig = platformAwareSdxConnector.getHmsServiceConfig(sdxBasicView.crn());
        try {
            LOGGER.info("Refreshing the database configuration.");
            apiConnectors.getConnector(dataHubStack).updateServiceConfig(service, getHmsDbConfigMap(hmsServiceConfig));
        } catch (CloudbreakException e) {
            LOGGER.info("Exception while updating the Data-Hub configuration", e);
        }
    }

    private Map<String, String> getHmsDbConfigMap(Map<String, String> configuration) {
        String port = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_PORT);
        String host = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_HOST);
        String dbName = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_NAME);
        String password = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_PASSWORD);
        String user = getHmsServiceConfigValue(configuration, HIVE_METASTORE_DATABASE_USER);
        String connectionUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        Map<String, String> configs = new HashMap<>();
        configs.put(HIVE_METASTORE_DATABASE_HOST, host);
        configs.put(HIVE_METASTORE_DATABASE_NAME, dbName);
        configs.put(HIVE_METASTORE_DATABASE_PASSWORD, password);
        configs.put(HIVE_METASTORE_DATABASE_PORT, port);
        configs.put(HIVE_METASTORE_DATABASE_TYPE, "postgresql");
        configs.put(HIVE_METASTORE_DATABASE_USER, user);
        configs.put(JDBC_URL_OVERRIDE, connectionUrl);
        return configs;
    }

    private String getHmsServiceConfigValue(Map<String, String> configuration, String key) {
        return Optional.ofNullable(configuration.get(key))
                .orElseThrow(() -> new NoSuchElementException(String.format("%s is not found in remote data context!", key)));
    }

    private boolean isDatalakeCreatedAfterDataHub(SdxBasicView sdxBasicView, Stack dataHubStack) {
        return sdxBasicView.created() > dataHubStack.getCreated();
    }

    private boolean shouldReloadDatabaseConfig(CmTemplateProcessor blueprintProcessor) {
        return blueprintProcessor.doesCMComponentExistsInBlueprint(AllRoleTypes.HIVEMETASTORE.name());
    }
}
