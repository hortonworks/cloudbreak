package com.sequenceiq.datalake.service.upgrade.database;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseServerParameterSetter;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
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
    private DatabaseService databaseService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseServerParameterSetterMap;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private SdxDatabaseRepository sdxDatabaseRepository;

    public SdxUpgradeDatabaseServerResponse upgrade(NameOrCrn sdxNameOrCrn, TargetMajorVersion requestedTargetMajorVersion, boolean forced) {
        LOGGER.debug("Upgrade database server called for {} with target major version {}", sdxNameOrCrn, requestedTargetMajorVersion);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByNameOrCrn(userCrn, sdxNameOrCrn);
        TargetMajorVersion targetMajorVersion = getTargetMajorVersion(requestedTargetMajorVersion, cluster);
        MDCBuilder.buildMdcContext(cluster);

        DatalakeStatusEnum status = sdxStatusService.getActualStatusForSdx(cluster).getStatus();
        if (isDatabaseServerUpgradeInProgress(status)) {
            throwUpgradeInProgressError(cluster, targetMajorVersion);
        }

        if (!isDatalakeAvailableForUpgade(status)) {
            throwDatalakeNotAvailableForUpgradeError(cluster, targetMajorVersion);
        }

        if (!isRuntimeVersionAllowedForUpgrade(cluster, targetMajorVersion)) {
            throwDatalakeRuntimeTooLowError(targetMajorVersion, cluster);
        }

        StackDatabaseServerResponse databaseResponse = databaseService.getDatabaseServer(cluster.getDatabaseCrn());
        if (!isUpgradeNeeded(targetMajorVersion, databaseResponse, forced)) {
            throwAlreadyOnLatestError(cluster);
        }

        if (!isDatabaseAvailableForUpgrade(databaseResponse)) {
            throwDatabaseNotAvailableForUpgradeError(cluster, databaseResponse);
        }

        cloudbreakStackService.checkUpgradeRdsByClusterNameInternal(cluster, targetMajorVersion);
        return triggerDatabaseUpgrade(cluster, targetMajorVersion, forced);
    }

    private TargetMajorVersion getTargetMajorVersion(TargetMajorVersion requestedTargetMajorVersion, SdxCluster cluster) {
        TargetMajorVersion targetMajorVersion = ObjectUtils.defaultIfNull(
                requestedTargetMajorVersion, defaultTargetMajorVersion);
        LOGGER.debug("Calculated upgrade target is {}, based on requested {}, general default {}",
                targetMajorVersion,
                requestedTargetMajorVersion,
                defaultTargetMajorVersion);
        return targetMajorVersion;
    }

    private boolean isUpgradeNeeded(TargetMajorVersion targetMajorVersion, StackDatabaseServerResponse databaseResponse, boolean forced) {
        return sdxDatabaseServerUpgradeAvailabilityService.isUpgradeNeeded(databaseResponse, targetMajorVersion, forced);
    }

    private boolean isDatabaseServerUpgradeInProgress(DatalakeStatusEnum status) {
        return status.isDatabaseServerUpgradeInProgress();
    }

    private boolean isRuntimeVersionAllowedForUpgrade(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        return databaseUpgradeRuntimeValidator.isRuntimeVersionAllowedForUpgrade(cluster.getRuntime(), targetMajorVersion.getMajorVersion());
    }

    private boolean isDatalakeAvailableForUpgade(DatalakeStatusEnum status) {
        return RUNNING == status || DATALAKE_UPGRADE_DATABASE_SERVER_FAILED == status;
    }

    private boolean isDatabaseAvailableForUpgrade(StackDatabaseServerResponse databaseResponse) {
        DatabaseServerStatus status = databaseResponse.getStatus();
        return status != null && status.isAvailableForUpgrade();
    }

    public void initUpgradeInCb(SdxCluster sdxCluster, TargetMajorVersion targetMajorVersion, boolean forced) {
        LOGGER.debug("Calling database server upgrade on stack endpoint for CRN {} for datalake {} to version {}. Forced: {}",
                sdxCluster.getStackCrn(), sdxCluster.getName(), targetMajorVersion.getMajorVersion(), forced);
        try {
            cloudbreakStackService.upgradeRdsByClusterNameInternal(sdxCluster, targetMajorVersion, forced);
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
            throwAlreadyOnLatestError(sdxCluster);
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
                .ifPresent(majorVersion -> updateVersionRelatedDatabaseParams(sdxCluster, majorVersion));
    }

    private void updateVersionRelatedDatabaseParams(SdxCluster sdxCluster, MajorVersion majorVersion) {
        sdxService.updateDatabaseEngineVersion(sdxCluster, majorVersion.getMajorVersion());
        if (sdxCluster.hasExternalDatabase() && StringUtils.isNotEmpty(sdxCluster.getDatabaseCrn())) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentClientService.getByCrn(sdxCluster.getEnvCrn()).getCloudPlatform());
            Optional<SdxDatabase> sdxDatabase = databaseServerParameterSetterMap.get(cloudPlatform)
                    .updateVersionRelatedDatabaseParameters(sdxCluster.getSdxDatabase(), majorVersion.getMajorVersion());
            sdxDatabase.ifPresent(database -> sdxDatabaseRepository.save(database));
        }
    }

    private void throwDatalakeNotAvailableForUpgradeError(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        throwBadRequestException(String.format("Data Lake %s is not available for database server upgrade", cluster.getName()));
    }

    private void throwDatabaseNotAvailableForUpgradeError(SdxCluster cluster, StackDatabaseServerResponse databaseResponse) {
        DatabaseServerStatus status = databaseResponse.getStatus();
        String msg = String.format("Upgrading database server of Data Lake %s is not possible as database server is not available", cluster.getName());
        msg += status == null ? "." : String.format(", it is in %s state.", status);
        throwBadRequestException(msg);
    }

    private void throwUpgradeInProgressError(SdxCluster cluster, TargetMajorVersion targetMajorVersion) {
        throwBadRequestException(String.format("Database server upgrade for Data Lake %s is already in progress", cluster.getName()));
    }

    private void throwAlreadyOnLatestError(SdxCluster cluster) {
        throwBadRequestException(String.format("Database server is already on the latest version for data lake %s", cluster.getName()));
    }

    private void throwDatalakeRuntimeTooLowError(TargetMajorVersion targetMajorVersion, SdxCluster cluster) {
        String errorMessage = String.format("The database upgrade of Data Lake %s is not permitted for runtime version %s.",
                cluster.getName(),
                cluster.getRuntime());
        Optional<String> minRuntimeVersion = databaseUpgradeRuntimeValidator.getMinRuntimeVersion(targetMajorVersion.getMajorVersion());
        if (minRuntimeVersion.isPresent()) {
            errorMessage += String.format(" The minimum supported runtime version is %s", minRuntimeVersion.get());
        }
        throwBadRequestException(errorMessage);
    }

    private void throwBadRequestException(String message) {
        LOGGER.warn(message);
        throw new BadRequestException(message);
    }

    private SdxUpgradeDatabaseServerResponse triggerDatabaseUpgrade(SdxCluster cluster, TargetMajorVersion targetMajorVersion, boolean forced) {
        LOGGER.debug("Triggering database server upgrade");
        sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_UPGRADE_DATABASE_SERVER_REQUESTED,
                Collections.singleton(targetMajorVersion.getMajorVersion()), "Database server upgrade requested",
                cluster.getId());
        FlowIdentifier flowIdentifier = reactorFlowManager.triggerDatabaseServerUpgradeFlow(cluster, targetMajorVersion, forced);
        LOGGER.info("RDS database server upgrade has been initiated for stack {} to version {}", cluster.getName(), targetMajorVersion.getMajorVersion());
        return new SdxUpgradeDatabaseServerResponse(flowIdentifier, targetMajorVersion);
    }

}
