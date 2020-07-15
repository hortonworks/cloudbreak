package com.sequenceiq.datalake.service.sdx.dr;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;

/**
 * Service to perform backup/restore of the database backing SDX.
 */
@Service
public class SdxDatabaseDrService {

    private static final int MAX_SIZE_OF_FAILURE_REASON = 254;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseDrService.class);

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

    public SdxDatabaseBackupResponse triggerDatabaseBackup(SdxCluster sdxCluster, String backupId, String backupLocation) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return triggerDatalakeDatabaseBackupFlow(sdxCluster.getId(), backupId, backupLocation);
    }

    public SdxDatabaseRestoreResponse triggerDatabaseRestore(SdxCluster sdxCluster, String backupId, String backupLocation) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return triggerDatalakeDatabaseRestoreFlow(sdxCluster.getId(), backupId, backupLocation);
    }

    private SdxDatabaseBackupResponse triggerDatalakeDatabaseBackupFlow(Long clusterId, String backupId, String backupLocation) {
        String selector = DATALAKE_DATABASE_BACKUP_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeDatabaseBackupStartEvent startEvent = new DatalakeDatabaseBackupStartEvent(selector, clusterId, userId, backupId, backupLocation);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(startEvent);
        return new SdxDatabaseBackupResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    private SdxDatabaseRestoreResponse triggerDatalakeDatabaseRestoreFlow(Long clusterId, String backupId, String backupLocation) {
        String selector = DATALAKE_DATABASE_RESTORE_EVENT.event();
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        DatalakeDatabaseRestoreStartEvent startEvent = new DatalakeDatabaseRestoreStartEvent(selector, clusterId, userId, backupId, backupLocation);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(startEvent);
        return new SdxDatabaseRestoreResponse(startEvent.getDrStatus().getOperationId(), flowIdentifier);
    }

    public void databaseBackup(SdxOperation drStatus, Long clusterId, String backupId, String backupLocation) {
        try {
            sdxOperationRepository.save(drStatus);
            sdxClusterRepository.findById(clusterId).ifPresentOrElse(sdxCluster -> {
                BackupV4Response backupV4Response = stackV4Endpoint.backupDatabaseByName(0L, sdxCluster.getClusterName(),
                        backupLocation, backupId, Crn.fromString(sdxCluster.getCrn()).getAccountId());
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

    public void databaseRestore(SdxOperation drStatus, Long clusterId, String backupId, String backupLocation) {
        try {
            sdxOperationRepository.save(drStatus);
            sdxClusterRepository.findById(clusterId).ifPresentOrElse(sdxCluster -> {
                RestoreV4Response restoreV4Response = stackV4Endpoint.restoreDatabaseByName(0L, sdxCluster.getClusterName(),
                        backupLocation, backupId, Crn.fromString(sdxCluster.getCrn()).getAccountId());
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
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
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
                CloudbreakFlowService.FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(sdxCluster);
                if (RUNNING.equals(flowState)) {
                    LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", pollingMessage, sdxCluster.getId());
                    return AttemptResults.justContinue();
                } else {
                    return getStackResponseAttemptResult(sdxCluster, pollingMessage, flowState);
                }
            }
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, String pollingMessage, CloudbreakFlowService.FlowState flowState)
            throws JsonProcessingException {
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(),
                Crn.fromString(sdxCluster.getCrn()).getAccountId());
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (isStackOrClusterDrStatusComplete(stackV4Response.getStatus()) && isStackOrClusterDrStatusComplete(cluster.getStatus())) {
            return sdxDrSucceeded(stackV4Response);
        } else if (isStackOrClusterStatusFailed(stackV4Response.getStatus())) {
            LOGGER.info("{} failed for Stack {} with status {}", pollingMessage, stackV4Response.getName(), stackV4Response.getStatus());
            return sdxDrFailed(sdxCluster, stackV4Response.getStatusReason(), pollingMessage);
        } else if (isStackOrClusterStatusFailed(stackV4Response.getCluster().getStatus())) {
            LOGGER.info("{} failed for Cluster {} status {}", pollingMessage, stackV4Response.getCluster().getName(),
                    stackV4Response.getCluster().getStatus());
            return sdxDrFailed(sdxCluster, stackV4Response.getCluster().getStatusReason(), pollingMessage);
        } else if (FINISHED.equals(flowState)) {
            LOGGER.info("Flow finished, but Backup/Restore is not complete: {}", sdxCluster.getClusterName());
            return sdxDrFailed(sdxCluster, "stack is in improper state", pollingMessage);
        } else {
            LOGGER.info("Flow is unknown state");
            return sdxDrFailed(sdxCluster, "Flow is unknown state", pollingMessage);
        }
    }

    /**
     * Checks the status if the backup/restore operation failed.
     * @param status It could be stack/cluster status
     * @return true if status is failed, false otherwise.
     */
    private boolean isStackOrClusterStatusFailed(Status status) {
        return Status.BACKUP_FAILED.equals(status) ||
                Status.RESTORE_FAILED.equals(status);
    }

    private AttemptResult<StackV4Response> sdxDrFailed(SdxCluster sdxCluster, String statusReason, String pollingMessage) {
        LOGGER.info("{} failed, statusReason: {}", pollingMessage, statusReason);
        return AttemptResults.breakFor("SDX " + pollingMessage + " failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private AttemptResult<StackV4Response> sdxDrSucceeded(StackV4Response stackV4Response) {
        LOGGER.info("Database DR operation does SDX cluster {} is successful", stackV4Response.getCluster().getName());
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
        if ((drStatus == null) || (!drStatus.getSdxClusterId().equals(sdxCluster.getId()))
                || (!drStatus.getOperationType().equals(type))) {
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
            case FAILED:
                return DatalakeDatabaseDrStatus.FAILED;
            default:
                return DatalakeDatabaseDrStatus.FAILED;
        }
    }
}
