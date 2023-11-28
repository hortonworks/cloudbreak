package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateDatabaseSettingsHandler extends ExceptionCatcherEventHandler<UpgradeRdsMigrateDatabaseSettingsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateDatabaseSettingsHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RdsSettingsMigrationService rdsSettingsMigrationService;

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
            Long clusterId = cluster.getId();
            Set<RDSConfig> rdsConfigs = rdsSettingsMigrationService.collectRdsConfigs(clusterId, this::isClouderaManager);
            rdsSettingsMigrationService.updateRdsConfigs(stackDto, rdsConfigs);
            rdsSettingsMigrationService.updateSaltPillars(stackDto, clusterId);
            rdsSettingsMigrationService.updateCMDatabaseConfiguration(stackDto);
        } catch (Exception e) {
            LOGGER.error("Migration of database settings is failed. {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsMigrateDatabaseSettingsResponse(stackId, request.getVersion());
    }

    private boolean isClouderaManager(RDSConfig rdsConfig) {
        return DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }
}