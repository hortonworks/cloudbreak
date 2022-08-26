package com.sequenceiq.datalake.service.upgrade.database;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxDatabaseResponseType;
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

    public SdxUpgradeDatabaseServerResponse upgrade(NameOrCrn sdxNameOrCrn, TargetMajorVersion requestedTargetMajorVersion) {
        LOGGER.debug("Upgrade database server called for {} with target major version {}", sdxNameOrCrn, requestedTargetMajorVersion);
        TargetMajorVersion targetMajorVersion = ObjectUtils.defaultIfNull(requestedTargetMajorVersion, defaultTargetMajorVersion);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByNameOrCrn(userCrn, sdxNameOrCrn);
        MDCBuilder.buildMdcContext(cluster);

        DatalakeStatusEnum status = sdxStatusService.getActualStatusForSdx(cluster).getStatus();
        if (status.isDatabaseServerUpgradeInProgress()) {
            return upgradeInProgressAnswer(cluster, targetMajorVersion);
        }

        if (RUNNING != status && DATALAKE_UPGRADE_DATABASE_SERVER_FAILED != status) {
            return datalakeNotAvailableForUpgradeAnswer(cluster, targetMajorVersion);
        }

        return sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(cluster, targetMajorVersion)
                ? triggerDatabaseUpgrade(cluster, targetMajorVersion)
                : alreadyOnLatestAnswer(cluster, targetMajorVersion);
    }

    public void initUpgradeInCb(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion) {
        LOGGER.debug("Calling database server upgrade on stack endpoint for CRN {} for datalake {}", sdxCluster.getStackCrn(), sdxCluster.getName());
        cloudbreakStackService.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion);
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

    private SdxUpgradeDatabaseServerResponse datalakeNotAvailableForUpgradeAnswer(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        String message = String.format("Data Lake %s is not available for database server upgrade", cluster.getName());
        LOGGER.info(message);
        return new SdxUpgradeDatabaseServerResponse(SdxDatabaseResponseType.ERROR, FlowIdentifier.notTriggered(), message, targetMajorVersion);
    }

    private SdxUpgradeDatabaseServerResponse upgradeInProgressAnswer(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        String message = String.format("Database server upgrade for Data Lake %s is already in progress", cluster.getName());
        LOGGER.debug(message);
        return new SdxUpgradeDatabaseServerResponse(SdxDatabaseResponseType.SKIP, FlowIdentifier.notTriggered(), message, targetMajorVersion);
    }

    private SdxUpgradeDatabaseServerResponse alreadyOnLatestAnswer(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        String message = String.format("Database server is already on the latest version for data lake %s", cluster.getName());
        LOGGER.info(message);
        return new SdxUpgradeDatabaseServerResponse(SdxDatabaseResponseType.SKIP, FlowIdentifier.notTriggered(), message, targetMajorVersion);
    }

    private SdxUpgradeDatabaseServerResponse triggerDatabaseUpgrade(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        LOGGER.debug("Triggering database server upgrade");
        sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED, "Database server upgrade requested",
                cluster.getId());
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDatabaseServerUpgradeFlow(cluster, targetMajorVersion);
        String reason = String.format("RDS database server upgrade has been initiated for stack %s", cluster.getName());
        LOGGER.info(reason);
        return new SdxUpgradeDatabaseServerResponse(SdxDatabaseResponseType.TRIGGERED, flowIdentifier, reason, targetMajorVersion);
    }

}
