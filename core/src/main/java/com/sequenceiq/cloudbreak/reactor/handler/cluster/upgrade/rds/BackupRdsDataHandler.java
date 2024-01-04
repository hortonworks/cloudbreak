package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class BackupRdsDataHandler extends ExceptionCatcherEventHandler<UpgradeRdsDataBackupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRdsDataHandler.class);

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsDataBackupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsDataBackupRequest> event) {
        LOGGER.error("Backup for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.AVAILABLE);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsDataBackupRequest> event) {
        UpgradeRdsDataBackupRequest request = event.getData();
        Long stackId = request.getResourceId();
        String backupLocation = request.getBackupLocation();
        String backupInstanceProfile = request.getBackupInstanceProfile();
        LOGGER.info("Starting backup for RDS upgrade {} {}...",
                StringUtils.isNotBlank(backupLocation) ? "and uploading to storage location " + backupLocation :  "",
                StringUtils.isNotBlank(backupInstanceProfile) ? "with using the backup instance profile " + backupInstanceProfile :  ""
        );
        try {
            upgradeRdsService.backupRds(stackId, backupLocation, backupInstanceProfile);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("RDS backup failed due to {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.AVAILABLE);
        }
        return new UpgradeRdsDataBackupResult(stackId, request.getVersion());
    }
}
