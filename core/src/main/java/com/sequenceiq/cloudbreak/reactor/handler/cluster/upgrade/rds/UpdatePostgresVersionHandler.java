package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdatePostgresVersionHandler extends ExceptionCatcherEventHandler<UpgradeRdsUpdateVersionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePostgresVersionHandler.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsUpdateVersionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsUpdateVersionRequest> event) {
        LOGGER.error("Database server engine version update failed. {}", e.getMessage());
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsUpdateVersionRequest> event) {
        UpgradeRdsUpdateVersionRequest request = event.getData();
        Long stackId = request.getResourceId();
        String targetMajorVersion = request.getVersion().getMajorVersion();
        LOGGER.info("Updating database engine version to {}", targetMajorVersion);
        try {
            stackUpdater.updateExternalDatabaseEngineVersion(stackId, targetMajorVersion);
            rdsUpgradeOrchestratorService.updateDatabaseEngineVersion(stackId);
        } catch (Exception e) {
            LOGGER.error("Database server engine version update failed. {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
        return new UpgradeRdsUpdateVersionResult(stackId, request.getVersion());
    }
}
