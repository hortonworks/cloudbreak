package com.sequenceiq.datalake.service.sdx.dr;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.flowcheck.FlowState.RUNNING;
import static java.util.Objects.isNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus.State;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxBackupResponse;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxRestoreStatusResponse;

/**
 * Service to perform backup/restore of the database backing SDX.
 */
@Service
public class SdxBackupRestoreService {

    private static final int MAX_SIZE_OF_FAILURE_REASON = 1999;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxBackupRestoreService.class);

    @Value("${last.backup.seconds:86400}")
    private int lastBackupInSeconds;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxOperationRepository sdxOperationRepository;

    @Inject
    private DatalakeDrClient datalakeDrClient;

    @Inject
    private DatalakeDrConfig datalakeDrConfig;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private SdxService sdxService;

    private static boolean isVersionOlderThan(SdxCluster cluster, String baseVersion) {
        LOGGER.info("Compared: String version {} with Versioned {}", cluster.getRuntime(), baseVersion);
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(cluster::getRuntime, () -> baseVersion) < 0;
    }

    private static boolean isVersionEqual(SdxCluster cluster, String baseVersion) {
        LOGGER.info("Compared: String version {} with Versioned {}", cluster.getRuntime(), baseVersion);
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(cluster::getRuntime, () -> baseVersion) == 0;
    }

    public SdxDatabaseBackupResponse triggerDatabaseBackup(SdxCluster sdxCluster, SdxDatabaseBackupRequest backupRequest) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return triggerDatalakeDatabaseBackupFlow(sdxCluster, backupRequest);
    }

    public SdxBackupResponse triggerDatalakeBackup(SdxCluster sdxCluster, String backupLocation, String backupName,
            DatalakeDrSkipOptions skipOptions, int fullDrMaxDurationInMin) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return triggerDatalakeBackupFlow(sdxCluster, backupLocation, backupName, skipOptions, fullDrMaxDurationInMin);
    }

    public DatalakeBackupStatusResponse triggerDatalakeBackup(Long id, String backupLocation, String backupName, String userCrn,
            DatalakeDrSkipOptions skipOptions) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        LOGGER.info("Triggering datalake backup for datalake: '{}' in '{}' env",
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        return datalakeDrClient.triggerBackup(
                sdxCluster.getClusterName(), backupLocation, backupName, userCrn, skipOptions);
    }

    public DatalakeBackupStatusResponse triggerDatalakeBackupValidation(Long id, String backupLocation, String userCrn) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        LOGGER.info("Triggering datalake backup validation for datalake: '{}' in '{}' env",
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        return datalakeDrClient.triggerBackupValidation(sdxCluster.getClusterName(), backupLocation, userCrn);
    }

    public SdxDatabaseRestoreResponse triggerDatabaseRestore(SdxCluster sdxCluster, String backupId, String restoreId, String backupLocation,
            int databaseMaxDurationInMin) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return triggerDatalakeDatabaseRestoreFlow(sdxCluster, backupId, restoreId, backupLocation, databaseMaxDurationInMin);
    }

    public SdxRestoreResponse triggerDatalakeRestore(SdxCluster sdxCluster, String datalakeName, String backupId, String backupLocationOverride,
            DatalakeDrSkipOptions skipOptions, int fullDrMaxDurationInMin, boolean validationOnly) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String backupLocation;
        MDCBuilder.buildMdcContext(sdxCluster);
        if (!Strings.isNullOrEmpty(backupId)) {
            datalakeDRProto.DatalakeBackupInfo backupInfo = datalakeDrClient.getBackupById(datalakeName, backupId, userCrn);
            if (backupInfo == null) {
                LOGGER.error("Backup Id {} does not exist for data lake: {}", backupId, datalakeName);
                throw new NotFoundException(String.format("Backup Id %s does not exist for data lake: %s", backupId, datalakeName));
            }
            backupLocation = backupInfo.getBackupLocation();
        } else {
            datalakeDRProto.DatalakeBackupInfo lastSuccessBackupInfo = getLastSuccessfulBackupInfo(datalakeName, userCrn);
            backupId = lastSuccessBackupInfo.getBackupId();
            backupLocation = lastSuccessBackupInfo.getBackupLocation();
        }
        return triggerDatalakeRestoreFlow(sdxCluster, backupId, backupLocation, backupLocationOverride, skipOptions, fullDrMaxDurationInMin, validationOnly);
    }

    public DatalakeRestoreStatusResponse triggerDatalakeRestoreValidation(SdxCluster sdxCluster, String userCrn) {
        LOGGER.info("Triggering datalake restore validation for datalake: '{}' in '{}' env",
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        return datalakeDrClient.triggerRestoreValidation(sdxCluster.getClusterName(), userCrn);
    }

    public datalakeDRProto.DatalakeBackupInfo getLastSuccessfulBackupInfo(String datalakeName, String userCrn) {
        datalakeDRProto.DatalakeBackupInfo lastSuccessfulBackupInfo = datalakeDrClient.getLastSuccessfulBackup(datalakeName, userCrn, Optional.empty());
        if (lastSuccessfulBackupInfo == null) {
            LOGGER.error("No successful backup found for data lake: {}", datalakeName);
            throw new NotFoundException(String.format("No successful backup found for data lake: %s", datalakeName));
        }
        return lastSuccessfulBackupInfo;
    }

    public Optional<datalakeDRProto.DatalakeBackupInfo> getLastSuccessfulBackupInfoWithRuntime(String datalakeName, String userCrn, String runtime) {
        datalakeDRProto.DatalakeBackupInfo lastSuccessfulBackupInfo = datalakeDrClient.getLastSuccessfulBackup(datalakeName, userCrn, Optional.of(runtime));
        if (lastSuccessfulBackupInfo == null) {
            LOGGER.error("No successful backup found for data lake {} for runtime {}", datalakeName, runtime);
            return Optional.empty();
        }
        return Optional.of(lastSuccessfulBackupInfo);
    }

    private SdxDatabaseBackupResponse triggerDatalakeDatabaseBackupFlow(SdxCluster cluster, SdxDatabaseBackupRequest backupRequest) {
        String selector = DATALAKE_DATABASE_BACKUP_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeDatabaseBackupStartEvent startEvent = new DatalakeDatabaseBackupStartEvent(selector, cluster.getId(), userId, backupRequest);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(startEvent, cluster.getClusterName());
        return new SdxDatabaseBackupResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    private SdxBackupResponse triggerDatalakeBackupFlow(SdxCluster cluster, String backupLocation, String backupName,
            DatalakeDrSkipOptions skipOptions, int fullDrMaxDurationInMin) {
        String selector = DATALAKE_TRIGGER_BACKUP_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeTriggerBackupEvent startEvent = new DatalakeTriggerBackupEvent(selector, cluster.getId(), userId,
                backupLocation, backupName, skipOptions, DatalakeBackupFailureReason.USER_TRIGGERED, Collections.emptyList(),
                fullDrMaxDurationInMin);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeBackupFlow(startEvent, cluster.getClusterName());
        return new SdxBackupResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    private SdxDatabaseRestoreResponse triggerDatalakeDatabaseRestoreFlow(SdxCluster cluster, String backupId, String restoreId, String backupLocation,
            int databaseMaxDurationInMin) {
        String selector = DATALAKE_DATABASE_RESTORE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeDatabaseRestoreStartEvent startEvent = new DatalakeDatabaseRestoreStartEvent(selector, cluster.getId(),
                userId, backupId, restoreId, backupLocation, databaseMaxDurationInMin, false);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(startEvent, cluster.getClusterName());
        return new SdxDatabaseRestoreResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    private SdxRestoreResponse triggerDatalakeRestoreFlow(SdxCluster cluster, String backupId, String backupLocation, String backupLocationOverride,
            DatalakeDrSkipOptions skipOptions, int fullDrMaxDurationInMin, boolean validationOnly) {
        String selector = DATALAKE_TRIGGER_RESTORE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeTriggerRestoreEvent startEvent = new DatalakeTriggerRestoreEvent(selector, cluster.getId(), cluster.getClusterName(), userId,
                backupId, backupLocation, backupLocationOverride, skipOptions, DatalakeRestoreFailureReason.USER_TRIGGERED, fullDrMaxDurationInMin,
                validationOnly);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeRestoreFlow(startEvent, cluster.getClusterName());
        return new SdxRestoreResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    public void databaseBackup(SdxOperation drStatus, Long clusterId, SdxDatabaseBackupRequest backupRequest) {
        try {
            sdxOperationRepository.save(drStatus);
            sdxClusterRepository.findById(clusterId).ifPresentOrElse(sdxCluster -> {
                String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
                BackupV4Response backupV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> stackV4Endpoint.backupDatabaseByNameInternal(0L, sdxCluster.getClusterName(),
                                backupRequest.getBackupId(), backupRequest.getBackupLocation(), backupRequest.isCloseConnections(),
                                backupRequest.getSkipDatabaseNames(), initiatorUserCrn, backupRequest.getDatabaseMaxDurationInMin(), false));
                updateSuccessStatus(drStatus.getOperationId(), sdxCluster, backupV4Response.getFlowIdentifier(),
                        SdxOperationStatus.TRIGGERRED);
            }, () -> {
                updateFailureStatus(drStatus.getOperationId(), clusterId, String.format("SDX cluster with Id [%d] not found", clusterId));
            });
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Database backup failed for datalake-id: [%d]. Message: [%s]", clusterId, errorMessage);
            throw new CloudbreakApiException(message);
        }
    }

    public void databaseRestore(SdxOperation drStatus, Long clusterId, String backupId, String backupLocation, int databaseMaxDurationInMin, boolean dryRun) {
        try {
            sdxOperationRepository.save(drStatus);
            sdxClusterRepository.findById(clusterId).ifPresentOrElse(sdxCluster -> {
                String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
                RestoreV4Response restoreV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> stackV4Endpoint.restoreDatabaseByNameInternal(0L, sdxCluster.getClusterName(),
                                backupLocation, backupId, initiatorUserCrn, databaseMaxDurationInMin, dryRun));
                updateSuccessStatus(drStatus.getOperationId(), sdxCluster, restoreV4Response.getFlowIdentifier(),
                        SdxOperationStatus.TRIGGERRED);
            }, () -> {
                updateFailureStatus(drStatus.getOperationId(), clusterId, String.format("SDX cluster with Id [%d] not found", clusterId));
            });
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Database restore failed for datalake-id: [%d]. Message: [%s]", clusterId, errorMessage);
            throw new CloudbreakApiException(message);
        }
    }

    public void waitCloudbreakFlow(Long id, PollingConfig pollingConfig, String pollingMessage) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkDatabaseDrStatus(sdxCluster, pollingMessage));
    }

    private AttemptResult<StackV4Response> checkDatabaseDrStatus(SdxCluster sdxCluster, String pollingMessage) throws JsonProcessingException {
        LOGGER.info("{} polling cloudbreak for stack status: '{}' in '{}' env", pollingMessage, sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in in-memory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(sdxCluster);
                if (RUNNING.equals(flowState)) {
                    LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", pollingMessage, sdxCluster.getId());
                    return AttemptResults.justContinue();
                } else {
                    return getStackResponseAttemptResult(sdxCluster, pollingMessage, flowState);
                }
            }
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    protected AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, String pollingMessage, FlowState flowState) {
        StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
        LOGGER.info("Response from cloudbreak: {}", stackV4Response);
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (isStackOrClusterDrStatusComplete(stackV4Response.getStatus()) && isStackOrClusterDrStatusComplete(cluster.getStatus())) {
            return sdxDrSucceeded(stackV4Response);
        } else if (isStackOrClusterStatusFailed(stackV4Response.getStatus())) {
            LOGGER.info("{} failed for Stack {} with status {}", pollingMessage, stackV4Response.getName(), stackV4Response.getStatus());
            return sdxDrFailed(sdxCluster, stackV4Response.getStatusReason(), pollingMessage);
        } else if (isStackOrClusterStatusFailed(cluster.getStatus())) {
            LOGGER.info("{} failed for Cluster {} status {}", pollingMessage, cluster.getName(), cluster.getStatus());
            return sdxDrFailed(sdxCluster, cluster.getStatusReason(), pollingMessage);
        } else if (FINISHED.equals(flowState)) {
            LOGGER.info("Flow finished, but Backup/Restore is not complete: {}", sdxCluster.getClusterName());
            return sdxDrFailed(sdxCluster, "stack is in improper state", pollingMessage);
        } else {
            LOGGER.info("{} for Stack {} failed with unhandled status: {}", pollingMessage, stackV4Response.getName(), stackV4Response.getStatus());
            return sdxDrFailed(sdxCluster, stackV4Response.getStatusReason(), pollingMessage);
        }
    }

    /**
     * Get which step the program is at when the backup/restore operation fails.
     *
     * @param sdxId The ID of the cluster.
     * @return A string of the information about where the operation fails.
     */
    public String createDatabaseBackupRestoreErrorStage(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        FlowCheckResponse lastKnownFlowCheckResponse = cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster);
        String errorStageInfo = "";
        if (lastKnownFlowCheckResponse != null && lastKnownFlowCheckResponse.getCurrentState() != null) {
            String state = String.format(" during the transition from %s to its next state", lastKnownFlowCheckResponse.getCurrentState());
            String flowType = "";
            if (lastKnownFlowCheckResponse.getFlowType() != null) {
                String[] flowTypeList = lastKnownFlowCheckResponse.getFlowType().split("\\.");
                flowType = String.format(", set up in %s", flowTypeList[flowTypeList.length - 1]);
            }
            String triggeredEvent = lastKnownFlowCheckResponse.getNextEvent() != null ?
                    String.format(", triggered by %s", lastKnownFlowCheckResponse.getNextEvent()) : "";
            errorStageInfo = String.format("%s%s%s", state, flowType, triggeredEvent);
        }
        return errorStageInfo;
    }

    /**
     * Checks the status if the backup/restore operation failed.
     *
     * @param status It could be stack/cluster status
     * @return true if status is failed, false otherwise.
     */
    private boolean isStackOrClusterStatusFailed(Status status) {
        return Status.BACKUP_FAILED.equals(status) ||
                Status.RESTORE_FAILED.equals(status) ||
                Status.UPDATE_FAILED.equals(status);
    }

    private AttemptResult<StackV4Response> sdxDrFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor(statusReason);
    }

    private AttemptResult<StackV4Response> sdxDrSucceeded(StackV4Response stackV4Response) {
        LOGGER.info("Database DR operation for SDX cluster {} is successful", stackV4Response.getCluster().getName());
        return AttemptResults.finishWith(stackV4Response);
    }

    private boolean isStackOrClusterDrStatusComplete(Status status) {
        return Status.AVAILABLE.equals(status);
    }

    public void updateDatabaseStatusEntry(String operationId, SdxOperationStatus status, String failedReason) {
        if (Strings.isNullOrEmpty(operationId)) {
            LOGGER.error("Operation-id is empty. Database Dr status will not be updated properly");
            return;
        }
        SdxOperation drStatus = sdxOperationRepository.findSdxOperationByOperationId(operationId);
        drStatus.setStatus(status);
        if (!Strings.isNullOrEmpty(failedReason)) {
            drStatus.setStatusReason(failedReason.substring(0, Math.min(failedReason.length(), MAX_SIZE_OF_FAILURE_REASON)));
        }
        sdxOperationRepository.save(drStatus);
    }

    public SdxDatabaseBackupStatusResponse getDatabaseBackupStatus(SdxCluster sdxCluster, String operationId) {
        MDCBuilder.buildMdcContext(sdxCluster);
        SdxOperation drStatus = getDatabaseDrStatus(sdxCluster, operationId,
                SdxOperationType.BACKUP);
        return new SdxDatabaseBackupStatusResponse(convertSdxDatabaseDrStatus(drStatus.getStatus()), drStatus.getStatusReason());
    }

    public SdxDatabaseRestoreStatusResponse getDatabaseRestoreStatus(SdxCluster sdxCluster, String operationId) {
        MDCBuilder.buildMdcContext(sdxCluster);
        SdxOperation drStatus = getDatabaseDrStatus(sdxCluster, operationId,
                SdxOperationType.RESTORE);
        return new SdxDatabaseRestoreStatusResponse(convertSdxDatabaseDrStatus(drStatus.getStatus()), drStatus.getStatusReason());
    }

    private void updateSuccessStatus(String operationId, SdxCluster sdxCluster, FlowIdentifier flowIdentifier,
            SdxOperationStatus status) {
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        updateDatabaseStatusEntry(operationId, status, null);
    }

    private void updateFailureStatus(String operationId, Long clusterId, String failure) {
        updateDatabaseStatusEntry(operationId, SdxOperationStatus.FAILED,
                failure);
        throw notFound("SDX cluster", clusterId).get();
    }

    private SdxOperation getDatabaseDrStatus(SdxCluster sdxCluster, String operationId,
            SdxOperationType type) {
        SdxOperation drStatus = sdxOperationRepository.findSdxOperationByOperationId(operationId);
        if (drStatus == null) {
            throw new NotFoundException(String.format("Status with id: [%s] not found", operationId));
        }
        if (!drStatus.getSdxClusterId().equals(sdxCluster.getId())
                || !drStatus.getOperationType().equals(type)) {
            String message = String.format("Invalid operation-id: [%s]. provided", operationId);
            throw new CloudbreakApiException(message);
        }
        return drStatus;
    }

    private DatalakeDatabaseDrStatus convertSdxDatabaseDrStatus(SdxOperationStatus status) {
        switch (status) {
            case INIT:
                return DatalakeDatabaseDrStatus.INIT;
            case TRIGGERRED:
                return DatalakeDatabaseDrStatus.TRIGGERRED;
            case INPROGRESS:
                return DatalakeDatabaseDrStatus.INPROGRESS;
            case SUCCEEDED:
                return DatalakeDatabaseDrStatus.SUCCEEDED;
            default:
                return DatalakeDatabaseDrStatus.FAILED;
        }
    }

    public void waitForServiceToBeStopped(Long id, String operationId, String userCrn, PollingConfig pollingConfig,
            String pollingMessage, SdxOperationType type) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkIfStopServicesHasCompleted(sdxCluster, operationId, userCrn, pollingMessage, type));
    }

    private AttemptResult<DatalakeOperationStatus> checkIfStopServicesHasCompleted(SdxCluster sdxCluster, String operationId,
            String userCrn, String pollingMessage, SdxOperationType type) {
        LOGGER.info("{} polling datalake-dr service for status: '{}' in '{}' env", pollingMessage,
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in in-memory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                DatalakeOperationStatus statusResponse = (type.equals(SdxOperationType.BACKUP)) ?
                        datalakeDrClient.getBackupStatusByBackupId(sdxCluster.getClusterName(), operationId, userCrn) :
                        datalakeDrClient.getRestoreStatusByRestoreId(sdxCluster.getClusterName(), operationId, userCrn);

                if (statusResponse.getIncludedData().contains("STOP_SERVICES")) {
                    LOGGER.info("Stopped services process for SDX cluster {} is successful", sdxCluster.getClusterName());
                    return AttemptResults.finishWith(statusResponse);
                } else if (statusResponse.isComplete()) {
                    LOGGER.info("Full DR operation for SDX cluster {} is successfully completed", sdxCluster.getClusterName());
                    return AttemptResults.finishWith(statusResponse);
                } else if (statusResponse.isFailed()) {
                    LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusResponse.getFailureReason());
                    return AttemptResults.breakFor(statusResponse.getFailureReason());
                } else if (statusResponse.getState() == State.CANCELLED) {
                    LOGGER.info("Full DR operation for SDX cluster {} is cancelled", sdxCluster.getClusterName());
                    return AttemptResults.finishWith(statusResponse);
                } else {
                    LOGGER.info("Datalake {} wait for stopped services still in progress", sdxCluster.getClusterName());
                    return AttemptResults.justContinue();
                }
            }
        } catch (Exception e) {
            return AttemptResults.breakFor(e);
        }
    }

    public DatalakeBackupStatusResponse waitForDatalakeDrBackupToComplete(Long id, String backupId, String userCrn, PollingConfig pollingConfig,
            String pollingMessage) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        DatalakeBackupStatusResponse response = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> getBackupStatusAttemptResult(sdxCluster, backupId, userCrn, pollingMessage));
        return response;
    }

    private AttemptResult<DatalakeBackupStatusResponse> getBackupStatusAttemptResult(SdxCluster sdxCluster, String backupId,
            String userCrn, String pollingMessage) {
        LOGGER.info("{} polling datalake-dr service for backup status: '{}' in '{}' env", pollingMessage,
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in in-memory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                DatalakeBackupStatusResponse response = datalakeDrClient.getBackupStatusByBackupId(
                        sdxCluster.getClusterName(), backupId, userCrn);
                if (response.getState().isNotComplete()) {
                    LOGGER.info("Datalake {} backup still in progress", sdxCluster.getClusterName());
                    return AttemptResults.justContinue();
                } else {
                    LOGGER.info("Backup for datalake {} complete with status {}", sdxCluster.getClusterName(), response.getState());
                    if (response.isFailed()) {
                        return sdxFullDrFailed(sdxCluster, response.getFailureReason(), pollingMessage);
                    } else if (response.getState() == State.CANCELLED) {
                        return sdxFullDrCancelled(sdxCluster, response);
                    } else {
                        return sdxFullDrSucceeded(sdxCluster, response);
                    }
                }
            }
        } catch (Exception e) {
            return AttemptResults.breakFor(e);
        }
    }

    public SdxBackupStatusResponse getDatalakeBackupStatus(String datalakeName, String backupId, String backupName, String userCrn) {
        LOGGER.info("Requesting datalake backup status for datalake: '{}'", datalakeName);
        DatalakeBackupStatusResponse datalakeBackupStatusResponse = datalakeDrClient.getBackupStatus(
                datalakeName, backupId, backupName, userCrn);
        return new SdxBackupStatusResponse(datalakeBackupStatusResponse.getBackupId(),
                datalakeBackupStatusResponse.getState().name(),
                datalakeBackupStatusResponse.getFailureReason(),
                datalakeBackupStatusResponse.getIncludedData(),
                datalakeBackupStatusResponse.getTimestamp(),
                datalakeBackupStatusResponse.getEndTimeStamp());
    }

    public String getDatalakeBackupId(String datalakeName, String backupName, String userCrn) {
        LOGGER.info("Requesting datalake backup Id for datalake: '{}'", datalakeName);
        return datalakeDrClient.getBackupId(datalakeName, backupName, userCrn);
    }

    public DatalakeRestoreStatusResponse triggerDatalakeRestore(Long id, String backupId, String backupLocationOverride, String userCrn,
            DatalakeDrSkipOptions skipOptions, boolean validationOnly) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        LOGGER.info("Triggering datalake restore for datalake: '{}' in '{}' env from backupId '{}",
                sdxCluster.getClusterName(), sdxCluster.getEnvName(), backupId);
        return datalakeDrClient.triggerRestore(
                sdxCluster.getClusterName(), backupId, backupLocationOverride, userCrn, skipOptions, validationOnly);
    }

    public SdxRestoreStatusResponse getDatalakeRestoreStatus(String datalakeName, String restoreId, String backupName, String userCrn) {
        LOGGER.info("Requesting datalake restore status for datalake: '{}' with restoreId '{}'", datalakeName, restoreId);
        DatalakeRestoreStatusResponse datalakeBackupStatusResponse = datalakeDrClient.getRestoreStatus(
                datalakeName, restoreId, backupName, userCrn);
        return new SdxRestoreStatusResponse(datalakeBackupStatusResponse.getBackupId(),
                datalakeBackupStatusResponse.getState().name(),
                datalakeBackupStatusResponse.getFailureReason());
    }

    public String getDatalakeRestoreId(String datalakeName, String backupName, String userCrn) {
        LOGGER.info("Requesting datalake restore Id for datalake: '{}'", datalakeName);
        return datalakeDrClient.getRestoreId(datalakeName, backupName, userCrn);
    }

    public void waitForDatalakeDrRestoreToComplete(Long id, String restoreId, String userCrn, PollingConfig pollingConfig,
            String pollingMessage) {
        SdxCluster sdxCluster = sdxClusterRepository.findById(id).orElseThrow(notFound("SDX cluster", id));
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> getRestoreStatusAttemptResult(sdxCluster, restoreId, userCrn, pollingMessage));
    }

    private AttemptResult<DatalakeBackupStatusResponse> getRestoreStatusAttemptResult(SdxCluster sdxCluster, String restoreId,
            String userCrn, String pollingMessage) {
        LOGGER.info("{} polling datalake-dr service for restore status: '{}' in '{}' env", pollingMessage,
                sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("{} polling cancelled in in-memory store, id: {}", pollingMessage, sdxCluster.getId());
                return AttemptResults.breakFor(pollingMessage + " polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else {
                DatalakeRestoreStatusResponse response = datalakeDrClient.getRestoreStatusByRestoreId(
                        sdxCluster.getClusterName(), restoreId, userCrn);
                if (response.getState().isNotComplete()) {
                    LOGGER.info("Datalake {} restore still in progress", sdxCluster.getClusterName());
                    return AttemptResults.justContinue();
                } else {
                    LOGGER.info("Restore for datalake {} complete with status {}", sdxCluster.getClusterName(), response.getState());
                    if (response.isFailed()) {
                        return sdxFullDrFailed(sdxCluster, response.getFailureReason(), pollingMessage);
                    } else {
                        return sdxFullDrSucceeded(sdxCluster, response.toBackupStatusResponse());
                    }
                }
            }
        } catch (Exception e) {
            return AttemptResults.breakFor(e);
        }
    }

    private AttemptResult<DatalakeBackupStatusResponse> sdxFullDrFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor(statusReason);
    }

    private AttemptResult<DatalakeBackupStatusResponse> sdxFullDrSucceeded(SdxCluster sdxCluster, DatalakeBackupStatusResponse response) {
        LOGGER.info("Full DR operation for SDX cluster {} is successful", sdxCluster.getClusterName());
        return AttemptResults.finishWith(response);
    }

    private AttemptResult<DatalakeBackupStatusResponse> sdxFullDrCancelled(SdxCluster sdxCluster, DatalakeBackupStatusResponse response) {
        LOGGER.info("Full DR operation for SDX cluster {} is cancelled", sdxCluster.getClusterName());
        return AttemptResults.finishWith(response);
    }

    /**
     * Checks if Sdx backup can be performed.
     *
     * @param cluster Sdx cluster.
     * @return true if backup can be performed, False otherwise.
     */
    public boolean shouldSdxBackupBePerformed(SdxCluster cluster) {
        String reason = null;
        if (!datalakeDrConfig.isConfigured()) {
            reason = "Datalake DR is not configured!";
        } else {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(cluster.getEnvName());
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentResponse.getCloudPlatform());
            if (CloudPlatform.MOCK.equalsIgnoreCase(cloudPlatform.name())) {
                return true;
            }

            if (isVersionOlderThan(cluster, "7.2.1")) {
                reason = "Unsupported runtime: " + cluster.getRuntime();
            } else if (cluster.getCloudStorageFileSystemType() == null) {
                reason = "Cloud storage not initialized";
            } else if (cluster.getCloudStorageFileSystemType().isAdlsGen2() &&
                    isVersionOlderThan(cluster, "7.2.2")) {
                reason = "Unsupported cloud provider Azure on runtime: " + cluster.getRuntime();
            }
        }
        if (reason != null) {
            LOGGER.info("Backup not triggered. Reason: " + reason);
            return false;
        }
        return true;
    }

    public boolean validationOnlyShouldBePerformed(boolean validationOnly, boolean performBackup) {
        if (!datalakeDrConfig.isConfigured()) {
            return false;
        }
        if (!performBackup) {
            return false;
        }
        return validationOnly;
    }

    /**
     * Checks if Sdx restore can be performed.
     *
     * @param cluster            Sdx cluster.
     * @return true if restore can be performed, False otherwise.
     */
    public boolean shouldSdxRestoreBePerformed(SdxCluster cluster) {
        String reason = null;

        if (!shouldSdxBackupBePerformed(cluster)) {
            reason = "Restore not performed as backup is not performed.";
        } else {
            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(cluster.getEnvName());
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentResponse.getCloudPlatform());
            if (cluster.isRangerRazEnabled()) {
                if (isVersionOlderThan(cluster, "7.2.14")) {
                    reason = "Automatic restore is not supported for RAZ on : " + cluster.getRuntime();
                } else if (isVersionEqual(cluster, "7.2.14") && (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform.name()))) {
                    reason = "Automatic restore is not supported for RAZ (AWS) on: " + cluster.getRuntime();
                }
            }
        }
        if (reason != null) {
            LOGGER.info("Restore not triggered. Reason: " + reason);
            return false;
        }
        return true;
    }

    public void checkExistingBackup(SdxCluster newSdxCluster, String userId) {
        if (!datalakeDrConfig.isConfigured()) {
            return;
        }
        datalakeDRProto.DatalakeBackupInfo lastSuccessfulBackup = datalakeDrClient
                .getLastSuccessfulBackup(newSdxCluster.getClusterName(), userId, Optional.empty());
        if (isNull(lastSuccessfulBackup)) {
            throw new BadRequestException("The restore cannot be executed because there is no backup.");
        }
        Date backupTime = new Date(Long.parseLong(lastSuccessfulBackup.getEndTimestamp()));
        if (TimeUnit.DAYS.convert(new Date().getTime() - backupTime.getTime(), TimeUnit.MILLISECONDS) > 0) {
            throw new BadRequestException(String.format("The restore cannot be executed because the last backup is older than %d days", lastBackupInSeconds));
        }
    }

    public void submitDatalakeDataInfo(String operationId, String inputJson, String userId) {
        datalakeDRProto.SubmitDatalakeDataInfoResponse response;
        try {
            response = datalakeDrClient.submitDatalakeDataInfo(operationId, inputJson, userId);
        } catch (Exception ex) {
            LOGGER.error("Unable to properly parse datalake data info input '" + inputJson + "' and submit it.", ex);
            throw new BadRequestException("Unable to properly parse datalake data info input '" + inputJson + "' and submit it.", ex);
        }

        if (response == null) {
            LOGGER.error("Failed to submit datalake data info for operation: " + operationId);
            throw new CloudbreakApiException("Failed to submit datalake data info for operation: " + operationId);
        }

        LOGGER.info("Successfully submitted datalake data info for operation: " + operationId);
    }

    public boolean isDatalakeInBackupProgress(String sdxClusterName, String actorCrn) {
        if (sdxClusterName == null || actorCrn == null) {
            LOGGER.error("Didn't find datalake name or actor CRN. Can not validate if the datalake is in backup. Continue to next step.");
            return false;
        }
        try {
            DatalakeBackupStatusResponse backupStatus = datalakeDrClient.getBackupStatus(sdxClusterName, actorCrn);
            if (backupStatus != null && isRunningState(backupStatus.getState())) {
                LOGGER.error("Found a backup in progress on {} with status {}", sdxClusterName, backupStatus.getState());
                return true;
            }
            LOGGER.info("No backup in progress found on {}. Good to continue.", sdxClusterName);
            return false;
        } catch (RuntimeException exception) {
            if (exception.getMessage().contains("Status information for backup operation on datalake")
                    && exception.getMessage().contains("not found")) {
                // If the RuntimeException's error message matches the message that was thrown when no backup/restore status found, the program continues because
                // it means this datalake has never run a backup or restore.
                LOGGER.info("No backup ever happened on {}. Good to continue.", sdxClusterName);
            } else {
                // Checking the backup/restore status should not prevent the resize flow. Continue the process.
                LOGGER.error("Failed to check backup status: {}. Continue to next step.", exception.getMessage());
            }
            return false;
        }
    }

    public boolean isDatalakeInRestoreProgress(String sdxClusterName, String actorCrn) {
        if (sdxClusterName == null || actorCrn == null) {
            LOGGER.error("Didn't find datalake name or actor CRN. Can not validate if the datalake is in restore. Continue to next step.");
            return false;
        }
        try {
            DatalakeRestoreStatusResponse restoreStatus = datalakeDrClient.getRestoreStatus(sdxClusterName, actorCrn);
            if (restoreStatus != null && isRunningState(restoreStatus.getState())) {
                LOGGER.error("Found a restore in progress on {} with status {}", sdxClusterName, restoreStatus.getState());
                return true;
            }
            LOGGER.info("No restore in progress found on {}. Good to continue.", sdxClusterName);
            return false;
        } catch (RuntimeException exception) {
            if (exception.getMessage().contains("Status information for restore operation on datalake")
                    && exception.getMessage().contains("not found")) {
                LOGGER.info("No restore ever happened on {}. Good to continue.", sdxClusterName);
            } else {
                LOGGER.error("Failed to check restore status: {}. Continue to next step.", exception.getMessage());
            }
            return false;
        }
    }

    private boolean isRunningState(DatalakeOperationStatus.State state) {
        return state != null
                && (state == DatalakeOperationStatus.State.STARTED
                || state == DatalakeOperationStatus.State.IN_PROGRESS);
    }

    /**
     * Modify backup location if it is an AWS or AZURE RAZ environment.
     *
     * @param cluster        The SDX cluster.
     * @param backupLocation The original backup location.
     * @return The updated backup location.
     */
    public String modifyBackupLocation(SdxCluster cluster, String backupLocation) {
        LOGGER.info("Checking if it needs to modify backup location {}", backupLocation);
        DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(cluster.getEnvName());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentResponse.getCloudPlatform());
        boolean requireNonEmptyPath = cloudPlatform == CloudPlatform.AWS || cloudPlatform == CloudPlatform.AZURE;
        if (requireNonEmptyPath && cluster.isRangerRazEnabled()) {
            backupLocation = appendBackupsIfRootLocation(backupLocation);
        }
        LOGGER.info("Backup location after potential modification is: {}", backupLocation);
        return backupLocation;
    }

    /**
     * Append "/backups" or "backups" to root directory for RAZ. Below are the two types of root directories:
     * s3a://my-bucket or abfs://test@mydatalake.dfs.core.windows.net
     * s3a://my-bucket/ abfs://test@mydatalake.dfs.core.windows.net/
     *
     * @param backupLocation The original backup location.
     * @return The updated backup location.
     */
    private String appendBackupsIfRootLocation(String backupLocation) {
        try {
            URI uri = new URI(backupLocation);
            if (Strings.isNullOrEmpty(uri.getPath())) {
                LOGGER.info("Append '/backups' to backup location {}", backupLocation);
                return backupLocation + "/backups";
            }
            if ("/".equals(uri.getPath())) {
                LOGGER.info("Append 'backups' to backup location {}", backupLocation);
                return backupLocation + "backups";
            }
        } catch (URISyntaxException e) {
            LOGGER.warn("Cannot parse backup location {}", backupLocation, e);
        }
        return backupLocation;
    }

    public long getTotalDurationInMin(String startTimeStamp, String endTimeStamp) {
        try {
            if (startTimeStamp == null || startTimeStamp.isEmpty() ||
                    endTimeStamp == null || endTimeStamp.isEmpty()) {
                return 0;
            }

            OffsetDateTime startTime = OffsetDateTime.parse(startTimeStamp);
            OffsetDateTime endTime = OffsetDateTime.parse(endTimeStamp);

            Duration duration = Duration.between(startTime, endTime);
            return duration.toMinutes();
        } catch (Exception e) {
            LOGGER.error("Failed to parse backup startTimeStamp {} or endTimeStamp {}", startTimeStamp, endTimeStamp);
            return 0;
        }
    }
}
