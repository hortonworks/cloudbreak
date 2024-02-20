package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.AttachedDatahubsRdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateAttachedDatahubsDBSettingsHandler extends ExceptionCatcherEventHandler<UpgradeRdsMigrateAttachedDatahubsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateAttachedDatahubsDBSettingsHandler.class);

    @Inject
    private AttachedDatahubsRdsSettingsMigrationService attachedDatahubsRdsSettingsMigrationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsMigrateAttachedDatahubsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsMigrateAttachedDatahubsRequest> event) {
        LOGGER.error("Migration of attached datahubs' database settings is failed. {}", e.getMessage());
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsMigrateAttachedDatahubsRequest> event) {
        UpgradeRdsMigrateAttachedDatahubsRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Migrating attached datahubs' database settings...");
        try {
            attachedDatahubsRdsSettingsMigrationService.migrate(stackId);
        } catch (Exception e) {
            LOGGER.error("Migration of attached datahubs' database settings is failed. {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsMigrateAttachedDatahubsResponse(stackId, request.getVersion());
    }
}
