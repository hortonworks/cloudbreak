package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsResponse;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateDatabaseSettingsHandler extends ExceptionCatcherEventHandler<UpgradeRdsMigrateDatabaseSettingsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateDatabaseSettingsHandler.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private RdsConfigUpdateService rdsConfigUpdateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsMigrateDatabaseSettingsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsMigrateDatabaseSettingsRequest> event) {
        LOGGER.error("Database settings migration failed. {}", e.getMessage());
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsMigrateDatabaseSettingsRequest> event) {
        UpgradeRdsMigrateDatabaseSettingsRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Migrating database settings...");
        try {
            StackDto stackDto = stackDtoService.getById(request.getResourceId());
            ClusterView cluster = stackDto.getCluster();
            updateRdsConfigs(stackDto);
            updateSaltPillars(stackDto, cluster);
            updateCMDatabaseConfiguration(stackDto);
        } catch (Exception e) {
            LOGGER.error("Migration of database settings is failed. {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsMigrateDatabaseSettingsResponse(stackId, request.getVersion());
    }

    private void updateRdsConfigs(StackDto stackDto) {
        rdsConfigUpdateService.updateRdsConnectionUserName(stackDto);
    }

    private void updateSaltPillars(StackDto stackDto, ClusterView cluster) throws CloudbreakOrchestratorFailedException {
        Map<String, SaltPillarProperties> pillarPropertiesSet = Maps.newHashMap();
        pillarPropertiesSet.put(ClusterHostServiceRunner.CM_DATABASE_PILLAR_KEY,
                clusterHostServiceRunner.getClouderaManagerDatabasePillarProperties(cluster));
        pillarPropertiesSet.put(PostgresConfigService.POSTGRESQL_SERVER, postgresConfigService.getPostgreSQLServerPropertiesForRotation(stackDto));
        LOGGER.debug("Updating db related pillars for db server upgrade: {}", String.join(",", pillarPropertiesSet.keySet()));
        hostOrchestrator.saveCustomPillars(new SaltConfig(pillarPropertiesSet), exitCriteriaProvider.get(stackDto),
                saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR));
    }

    private void updateCMDatabaseConfiguration(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Updating CM db.properties configuration file according to the db settings changes.");
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        hostOrchestrator.executeSaltState(primaryGatewayConfig, Set.of(primaryGatewayConfig.getHostname()), List.of("cloudera.manager.update-db-user"),
                exitCriteriaProvider.get(stackDto), Optional.of(MAX_RETRY_ON_ERROR), Optional.of(MAX_RETRY_ON_ERROR));
    }

}
