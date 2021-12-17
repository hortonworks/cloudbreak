package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbCertificateProvider;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStartHandler implements EventHandler<ClusterStartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartHandler.class);

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
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private RedbeamsDbCertificateProvider dbCertificateProvider;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartRequest.class);
    }

    @Override
    public void accept(Event<ClusterStartRequest> event) {
        ClusterStartRequest request = event.getData();
        ClusterStartResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Optional<Stack> datalakeStack = datalakeService.getDatalakeStackByDatahubStack(stack);
            int requestId;
            // Re-configuring DH using the Remote Data Context of Data lake.
            boolean clusterDataHub = stack.getType().equals(StackType.WORKLOAD);
            boolean dlIsRebuild = datalakeStack.isPresent() && isDatalakeCreatedAfterDataHub(datalakeStack.get(), stack);
            boolean resizeEntitlementEnabled = entitlementService.isDatalakeLightToMediumMigrationEnabled(ThreadBasedUserCrnProvider.getAccountId());
            LOGGER.info("Is cluster DH: {}, Found data lake stack: {}, Is DL rebuild: {},  Is resize entitlement Enabled: {}",
                    clusterDataHub,
                    datalakeStack.isPresent(),
                    dlIsRebuild,
                    resizeEntitlementEnabled);
            if (clusterDataHub && datalakeStack.isPresent() &&
                    dlIsRebuild &&
                    resizeEntitlementEnabled) {
                LOGGER.info("Triggering update of remote data context");
                apiConnectors.getConnector(stack).startClusterMgmtServices();
                clusterBuilderService.configureManagementServices(stack.getId());
                if (shouldReloadDatabaseConfig(stack.getCluster())) {
                    //Update Hive service database configuration
                    updateDatabaseConfiguration(datalakeStack, stack, HIVE_SERVICE, DatabaseType.HIVE);
                } else {
                    LOGGER.info("Database configuration is not refreshed");
                }
                requestId = apiConnectors.getConnector(stack).startClusterServices();
            } else {
                requestId = apiConnectors.getConnector(stack).startCluster();
            }
            result = new ClusterStartResult(request, requestId);
        } catch (Exception e) {
            result = new ClusterStartResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void updateDatabaseConfiguration(Optional<Stack> datalakeStack, Stack dataHubStack, String service, DatabaseType databaseType) {
        Cluster cluster = clusterService.getById(datalakeStack.get().getCluster().getId());
        Optional<RDSConfig> rdsConfig = postgresConfigService.createRdsConfigIfNeeded(datalakeStack.get(), cluster, databaseType)
                .stream().filter(config -> config.getType().toLowerCase().equals(databaseType.toString().toLowerCase()))
                .findFirst();
        try {
            if (rdsConfig.isPresent()) {
                apiConnectors.getConnector(dataHubStack).updateServiceConfig(service, getRdsConfigMap(rdsConfig.get()));
            } else {
                LOGGER.error("Could not find RDS configuration for Hive");
            }
        } catch (CloudbreakException e) {
            LOGGER.info("Exception while updating the Data-Hub configuration", e);
        }
    }

    private Map<String, String> getRdsConfigMap(RDSConfig rdsConfig) {
        RdsView hiveRdsView = new RdsView(rdsConfig, dbCertificateProvider.getSslCertsFilePath());
        Map<String, String> configs = new HashMap<String, String>();
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

    private boolean shouldReloadDatabaseConfig(Cluster cluster) {
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        CmTemplateProcessor blueprintProcessor = cmTemplateProcessorFactory.get(blueprintText);
        return blueprintProcessor.isCMComponentExistsInBlueprint("HIVEMETASTORE");
    }
}
