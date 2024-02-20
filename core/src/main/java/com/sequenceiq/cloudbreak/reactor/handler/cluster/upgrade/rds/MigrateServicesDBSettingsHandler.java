package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsResponse;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateServicesDBSettingsHandler extends ExceptionCatcherEventHandler<UpgradeRdsMigrateServicesDBSettingsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateServicesDBSettingsHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsMigrateServicesDBSettingsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsMigrateServicesDBSettingsRequest> event) {
        LOGGER.error("Services' db settings migration failed. {}", e.getMessage());
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsMigrateServicesDBSettingsRequest> event) {
        UpgradeRdsMigrateServicesDBSettingsRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Migrating services' database settings...");
        try {
            StackDto stackDto = stackDtoService.getById(request.getResourceId());
            ClusterView cluster = stackDto.getCluster();
            Set<RDSConfig> rdsConfigs = rdsSettingsMigrationService.collectRdsConfigs(cluster.getId(), this::isClouderaManagerService);
            rdsConfigs = rdsSettingsMigrationService.updateRdsConfigs(stackDto, rdsConfigs);
            Table<String, String, String> cmServiceConfigs = rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs);
            rdsSettingsMigrationService.updateCMServiceConfigs(stackDto, cmServiceConfigs, false);
        } catch (Exception e) {
            LOGGER.error("Migration of services' database settings is failed. {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsMigrateServicesDBSettingsResponse(stackId, request.getVersion());
    }

    private boolean isClouderaManagerService(RDSConfig rdsConfig) {
        return !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }
}
