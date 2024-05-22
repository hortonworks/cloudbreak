package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class InstallPostgresPackagesHandler extends ExceptionCatcherEventHandler<UpgradeRdsInstallPostgresPackagesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallPostgresPackagesHandler.class);

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsInstallPostgresPackagesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsInstallPostgresPackagesRequest> event) {
        LOGGER.error("Installing Postgres packages for RDS upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsInstallPostgresPackagesRequest> event) {
        UpgradeRdsInstallPostgresPackagesRequest request = event.getData();
        Long stackId = request.getResourceId();
        MajorVersion targetVersion = Optional.ofNullable(request.getVersion()).map(TargetMajorVersion::convertToMajorVersion).orElse(MajorVersion.VERSION_11);
        LOGGER.info("Installing Postgres [{}] packages for RDS upgrade...", targetVersion);
        try {
            upgradeRdsService.installPostgresPackages(stackId, targetVersion);
            return new UpgradeRdsInstallPostgresPackagesResult(stackId, request.getVersion());
        } catch (CloudbreakOrchestratorException e) {
            upgradeRdsService.handleInstallPostgresPackagesError(stackId, request.getVersion().getMajorVersion(), e.getMessage());
            LOGGER.warn("Installing Postgres packages failed due to {}", e.getMessage());
            return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
        }
    }
}
