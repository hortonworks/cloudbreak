package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.TargetMajorVersionToUpgradeTargetVersionConverter;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.UpgradeTargetMajorVersion;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Component
public class UpgradeRdsHandler extends ExceptionCatcherEventHandler<UpgradeRdsUpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRdsHandler.class);

    @Inject
    private ExternalDatabaseService databaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Inject
    private TargetMajorVersionToUpgradeTargetVersionConverter targetMajorVersionToUpgradeTargetVersionConverter;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeRdsUpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeRdsUpgradeDatabaseServerRequest> event) {
        LOGGER.error("RDS database server upgrade has failed", e);
        return new UpgradeRdsFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<UpgradeRdsUpgradeDatabaseServerRequest> event) {
        UpgradeRdsUpgradeDatabaseServerRequest request = event.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Starting RDS database upgrade...");
        StackDto stack = stackDtoService.getById(stackId);
        ClusterView cluster = stack.getCluster();
        try {
            if (StringUtils.isNotBlank(cluster.getDatabaseServerCrn())) {
                return upgradeExternalDatabase(request, stackId, stack);
            } else if (embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack)) {
                return upgradeEmbeddedDatabase(request, stackId, stack);
            } else {
                return upgradeFailedEvent(stackId, new IllegalStateException("Upgrade cannot be performed with embedded database present on root disk!"));
            }
        } catch (CloudbreakOrchestratorException e) {
            return upgradeFailedEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Exception during DB upgrade for stack: {}", stack.getName(), e);
            return upgradeFailedEvent(stackId, e);
        }
    }

    private UpgradeRdsUpgradeDatabaseServerResult upgradeExternalDatabase(UpgradeRdsUpgradeDatabaseServerRequest request, Long stackId, StackDto stackDto) {
        TargetMajorVersion targetMajorVersion = request.getVersion();
        UpgradeTargetMajorVersion upgradeTargetMajorVersion = targetMajorVersionToUpgradeTargetVersionConverter.convert(targetMajorVersion);
        DatabaseServerV4StackRequest migratedRequest = databaseService.migrateDatabaseSettingsIfNeeded(stackDto, targetMajorVersion);
        FlowIdentifier flowIdentifier = databaseService.upgradeDatabase(stackDto.getCluster(), upgradeTargetMajorVersion, migratedRequest);
        return new UpgradeRdsUpgradeDatabaseServerResult(stackId, request.getVersion(), flowIdentifier);
    }

    private UpgradeRdsUpgradeDatabaseServerResult upgradeEmbeddedDatabase(UpgradeRdsUpgradeDatabaseServerRequest request, Long stackId, StackDto stackDto)
            throws CloudbreakOrchestratorException {
        String targetMajorVersion = request.getVersion().getMajorVersion();
        LOGGER.debug("Starting embedded database upgrade to {} version...", targetMajorVersion);
        rdsUpgradeOrchestratorService.upgradeEmbeddedDatabase(stackDto, targetMajorVersion);
        LOGGER.debug("Upgrading embedded database version in db to {} version...", targetMajorVersion);
        stackUpdater.updateExternalDatabaseEngineVersion(stackId, targetMajorVersion);
        stackDto.getDatabase().setExternalDatabaseEngineVersion(targetMajorVersion);
        LOGGER.debug("Upgrading embedded database version in salt pillars to {} version...", targetMajorVersion);
        rdsUpgradeOrchestratorService.updateDatabaseEngineVersion(stackDto);
        return new UpgradeRdsUpgradeDatabaseServerResult(stackId, request.getVersion(), null);
    }

    private StackEvent upgradeFailedEvent(Long stackId, Exception e) {
        return new UpgradeRdsFailedEvent(stackId, e, DetailedStackStatus.DATABASE_UPGRADE_FAILED);
    }
}