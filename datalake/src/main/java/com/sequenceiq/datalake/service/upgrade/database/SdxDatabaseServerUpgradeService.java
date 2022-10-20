package com.sequenceiq.datalake.service.upgrade.database;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.datalake.service.validation.database.DatabaseUpgradeRuntimeValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxUpgradeDatabaseServerResponse;

@Service
public class SdxDatabaseServerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseServerUpgradeService.class);

    @Value("${sdx.db.env.upgrade.database.targetversion}")
    private TargetMajorVersion defaultTargetMajorVersion;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager reactorFlowManager;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private SdxDatabaseServerUpgradeAvailabilityChecker sdxDatabaseServerUpgradeAvailabilityService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Inject
    private DatabaseEngineVersionReaderService databaseEngineVersionReaderService;

    @Inject
    private DatabaseUpgradeRuntimeValidator databaseUpgradeRuntimeValidator;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public SdxUpgradeDatabaseServerResponse upgrade(NameOrCrn sdxNameOrCrn, TargetMajorVersion requestedTargetMajorVersion) {
        LOGGER.debug("Upgrade database server called for {} with target major version {}", sdxNameOrCrn, requestedTargetMajorVersion);
        TargetMajorVersion targetMajorVersion = ObjectUtils.defaultIfNull(requestedTargetMajorVersion, defaultTargetMajorVersion);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByNameOrCrn(userCrn, sdxNameOrCrn);
        MDCBuilder.buildMdcContext(cluster);

        DatalakeStatusEnum status = sdxStatusService.getActualStatusForSdx(cluster).getStatus();
        if (isDatabaseServerUpgradeInProgress(status)) {
            throwUpgradeInProgressError(cluster, targetMajorVersion);
        }

        if (isDatabaseAvailableForUpgade(status)) {
            throwDatalakeNotAvailableForUpgradeError(cluster, targetMajorVersion);
        }

        if (!isRuntimeVersionAllowedForUpgrade(cluster)) {
            throwDatalakeRuntimeTooLowError(targetMajorVersion, cluster);
        }

        if (!isUpgradeNeeded(targetMajorVersion, cluster)) {
            throwAlreadyOnLatestError(cluster, targetMajorVersion);
        }
        cloudbreakStackService.checkUpgradeRdsByClusterNameInternal(cluster, targetMajorVersion);
        return triggerDatabaseUpgrade(cluster, targetMajorVersion);
    }

    private boolean isUpgradeNeeded(TargetMajorVersion targetMajorVersion, SdxCluster cluster) {
        return sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(cluster, targetMajorVersion);
    }

    private boolean isDatabaseServerUpgradeInProgress(DatalakeStatusEnum status) {
        return status.isDatabaseServerUpgradeInProgress();
    }

    private boolean isRuntimeVersionAllowedForUpgrade(SdxCluster cluster) {
        return databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(cluster.getRuntime());
    }

    private boolean isDatabaseAvailableForUpgade(DatalakeStatusEnum status) {
        return RUNNING != status && DATALAKE_UPGRADE_DATABASE_SERVER_FAILED != status;
    }

    public void initUpgradeInCb(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion) {
        LOGGER.debug("Calling database server upgrade on stack endpoint for CRN {} for datalake {}", sdxCluster.getStackCrn(), sdxCluster.getName());
        try {
            cloudbreakStackService.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
        } catch (CloudbreakApiException exception) {
            handleIfAlreadyUpgradedOrThrow(sdxCluster, targetMajorVersion, exception);
        }
    }

    private void handleIfAlreadyUpgradedOrThrow(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion, CloudbreakApiException exception) {
        String message = exception.getMessage();
        String alreadyUpgradedMessage = cloudbreakMessagesService.getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_ALREADY_UPGRADED.getMessage(),
                List.of(targetMajorVersion.getMajorVersion()));
        if (message.contains(alreadyUpgradedMessage)) {
            updateDatabaseServerEngineVersion(sdxCluster);
            throwAlreadyOnLatestError(sdxCluster, targetMajorVersion);
        } else {
            throw exception;
        }
    }

    public void waitDatabaseUpgradeInCb(SdxCluster sdxCluster, PollingConfig pollingConfig) {
        LOGGER.debug("Waiting for database server upgrade on stack CRN {} for datalake {}", sdxCluster.getStackCrn(), sdxCluster.getName());
        cloudbreakPoller.pollDatabaseServerUpgradeUntilAvailable(sdxCluster, pollingConfig);
    }

    public void updateDatabaseServerEngineVersion(SdxCluster sdxCluster) {
        LOGGER.debug("Updating database server engine version  {} for datalake {}", sdxCluster.getStackCrn(), sdxCluster.getName());
        databaseEngineVersionReaderService.getDatabaseServerMajorVersion(sdxCluster)
                .ifPresent(majorVersion -> sdxService.updateDatabaseEngineVersion(sdxCluster.getCrn(), majorVersion.getMajorVersion()));
    }

    private void throwDatalakeNotAvailableForUpgradeError(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        throwBadRequestException(String.format("Data Lake %s is not available for database server upgrade", cluster.getName()));
    }

    private void throwUpgradeInProgressError(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        throwBadRequestException(String.format("Database server upgrade for Data Lake %s is already in progress", cluster.getName()));
    }

    private void throwAlreadyOnLatestError(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        throwBadRequestException(String.format("Database server is already on the latest version for data lake %s", cluster.getName()));
    }

    private void throwDatalakeRuntimeTooLowError(TargetMajorVersion targetMajorVersion, SdxCluster cluster) {
        throwBadRequestException(String.format("The database upgrade of Data Lake %s is not permitted for runtime version %s. The minimum supported runtime" +
                " version is %s", cluster.getName(), cluster.getRuntime(), databaseUpgradeRuntimeValidator.getMinRuntimeVersion()));
    }

    private void throwBadRequestException(String message) {
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private SdxUpgradeDatabaseServerResponse triggerDatabaseUpgrade(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        LOGGER.debug("Triggering database server upgrade");
        sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED,
                Collections.singleton(targetMajorVersion.getMajorVersion()), "Database server upgrade requested",
                cluster.getId());
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDatabaseServerUpgradeFlow(cluster, targetMajorVersion);
        LOGGER.info("RDS database server upgrade has been initiated for stack {}", cluster.getName());
        return new SdxUpgradeDatabaseServerResponse(flowIdentifier, targetMajorVersion);
    }

}
