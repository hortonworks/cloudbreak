package com.sequenceiq.redbeams.service.stack;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseResponse;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;
import com.sequenceiq.redbeams.service.network.NetworkBuilderService;
import com.sequenceiq.redbeams.service.operation.OperationService;

@Service
public class RedbeamsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private NetworkBuilderService networkBuilderService;

    @Inject
    private OperationService operationService;

    public UpgradeDatabaseResponse validateUpgradeDatabaseServer(String crn, UpgradeDatabaseRequest upgradeDatabaseRequest) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        MajorVersion currentVersion = dbStack.getMajorVersion();
        TargetMajorVersion targetVersion = upgradeDatabaseRequest.getTargetMajorVersion();

        LOGGER.debug("Validate upgrade called for: {}, with target version: {}, current version is: {}", dbStack, targetVersion, currentVersion);

        UpgradeDatabaseResponse runningFlowResponse = validateRunningFlow(crn, dbStack, currentVersion, Optional.of(targetVersion));
        if (runningFlowResponse != null) {
            return runningFlowResponse;
        }

        networkBuilderService.updateNetworkSubnets(dbStack);
        dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.VALIDATE_UPGRADE_REQUESTED);
        UpgradeDatabaseMigrationParams migrationParams = UpgradeDatabaseMigrationParams.fromDatabaseServer(upgradeDatabaseRequest.getMigratedDatabaseServer());
        RedbeamsStartValidateUpgradeRequest startRequest = new RedbeamsStartValidateUpgradeRequest(dbStack.getId(),
                    targetVersion, migrationParams);
        FlowIdentifier flowId = flowManager.notify(RedbeamsValidateUpgradeEvent.REDBEAMS_START_VALIDATE_UPGRADE_EVENT.selector(), startRequest);
        return new UpgradeDatabaseResponse(flowId, currentVersion);
    }

    public UpgradeDatabaseResponse validateUpgradeDatabaseServerCleanup(String crn) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);
        MajorVersion currentVersion = dbStack.getMajorVersion();

        LOGGER.debug("Validate upgrade cleanup called for: {}, current version is: {}", dbStack, currentVersion);

        UpgradeDatabaseResponse runningFlowResponse = validateRunningFlow(crn, dbStack, currentVersion, Optional.empty());
        if (runningFlowResponse != null) {
            return runningFlowResponse;
        }

        RedbeamsStartValidateUpgradeCleanupRequest startRequest = new RedbeamsStartValidateUpgradeCleanupRequest(dbStack.getId());
        FlowIdentifier flowId = flowManager.notify(RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_START_VALIDATE_UPGRADE_CLEANUP_EVENT.selector(), startRequest);
        return new UpgradeDatabaseResponse(flowId, currentVersion);
    }

    public UpgradeDatabaseResponse upgradeDatabaseServer(String crn, UpgradeDatabaseRequest upgradeDatabaseRequest) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.buildMdcContext(dbStack);

        MajorVersion currentVersion = dbStack.getMajorVersion();
        TargetMajorVersion targetVersion = upgradeDatabaseRequest.getTargetMajorVersion();

        LOGGER.debug("Upgrade called for: {}, with target version: {}, current version is: {}", dbStack, targetVersion, currentVersion);

        UpgradeDatabaseResponse runningFlowResponse = validateRunningFlow(crn, dbStack, currentVersion, Optional.of(targetVersion));
        if (runningFlowResponse != null) {
            return runningFlowResponse;
        }

        networkBuilderService.updateNetworkSubnets(dbStack);
        dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.UPGRADE_REQUESTED);
        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                targetVersion, UpgradeDatabaseMigrationParams.fromDatabaseServer(upgradeDatabaseRequest.getMigratedDatabaseServer()));
        FlowIdentifier flowId = flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest);
        return new UpgradeDatabaseResponse(flowId, currentVersion);
    }

    private UpgradeDatabaseResponse validateRunningFlow(String crn, DBStack dbStack, MajorVersion currentVersion, Optional<TargetMajorVersion> targetVersion) {
        if (dbStack.getStatus().isUpgradeInProgress()) {
            OperationView operationView = operationService.getOperationProgressByResourceCrn(crn, false);
            if (operationView.getProgressStatus().isRunning()) {
                return handleRunningFlow(crn, currentVersion, operationView);
            } else {
                LOGGER.warn("[INVESTIGATE] DatabaseServer with crn {} has {} status but no running flows so re-triggering the upgrade flow now", crn,
                        dbStack.getStatus());
            }
        } else if (targetVersion.isPresent() && !isUpgradeNeeded(currentVersion, targetVersion.get())) {
            return handleAlreadyUpgraded(crn, currentVersion);
        }
        return null;
    }

    private UpgradeDatabaseResponse handleAlreadyUpgraded(String crn, MajorVersion currentVersion) {
        String message = String.format("DatabaseServer with crn %s is already on version %s, upgrade is not necessary.",
                crn, currentVersion.getVersion());
        LOGGER.debug(message);
        return new UpgradeDatabaseResponse(message, currentVersion);
    }

    private UpgradeDatabaseResponse handleRunningFlow(String crn, MajorVersion currentVersion, OperationView operationView) {
        String message = String.format("DatabaseServer with crn %s is already being upgraded.", crn);
        LOGGER.debug(message);
        String operationId = operationView.getOperationId();
        List<FlowProgressResponse> operations = operationView.getOperations();
        FlowProgressResponse lastFlowProgress = operations.getLast();
        FlowIdentifier identifier = StringUtils.isBlank(lastFlowProgress.getFlowChainId()) ?
                new FlowIdentifier(FlowType.FLOW, operationId) :
                new FlowIdentifier(FlowType.FLOW_CHAIN, operationId);
        return new UpgradeDatabaseResponse(message, identifier, currentVersion);
    }

    private boolean isUpgradeNeeded(MajorVersion currentVersion, TargetMajorVersion targetMajorVersion) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        boolean upgradeNeeded = majorVersionComparator.compare(currentVersion, targetMajorVersion.convertToMajorVersion()) < 0;
        LOGGER.debug("Comparing current and target versions. Current version is {}, and target version is {}, isUpgradeNeeded: {}", currentVersion,
                targetMajorVersion, upgradeNeeded);
        return upgradeNeeded;
    }
}