package com.sequenceiq.datalake.job;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;

@DisallowConcurrentExecution
@Component
public class SdxClusterStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxClusterStatusCheckerJob.class);

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StatusCheckerJobService jobService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return Optional.ofNullable(sdxClusterRepository.findById(Long.valueOf(getLocalId())).orElse(null));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        if (getLocalId() != null && flowLogService.isOtherFlowRunning(Long.valueOf(getLocalId()))) {
            LOGGER.debug("Sdx StatusChecker Job cannot run, because flow is running for datalake: {}", getLocalId());
            return;
        }
        LOGGER.debug("Sdx StatusChecker Job is running for datalake: '{}'", getLocalId());
        if (getRemoteResourceCrn() == null) {
            jobService.unschedule(getLocalId());
            LOGGER.debug("Sdx StatusChecker Job is unscheduled for datalake: '{}'", getLocalId());
        } else {
            syncSdxStatus(context);
        }
    }

    private void syncSdxStatus(JobExecutionContext context) {
        getCluster().ifPresent(sdx -> {
            SdxStatusEntity sdxStatus = sdxStatusService.getActualStatusForSdx(sdx);
            if (DatalakeStatusEnum.STACK_DELETED.equals(sdxStatus.getStatus()) ||
                    (DatalakeStatusEnum.DELETED.equals(sdxStatus.getStatus()) && sdx.getDeleted() != null)) {
                LOGGER.info("Sdx status is {}. Unscheduling sdx sync job.", sdxStatus.getStatus());
                unscheduleSync(sdx);
            } else {
                StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(getRemoteResourceCrn());
                updateCertExpirationStateIfDifferent(sdx, stack);
                updateProviderSyncStateIfDifferent(sdx, stack);
                DatalakeStatusEnum originalStatus = sdxStatus.getStatus();
                DatalakeStatusEnum updatedStatus = updateStatusIfNecessary(stack, sdx, sdxStatus);
                if (!Objects.equals(originalStatus, updatedStatus)) {
                    logStateChange(originalStatus, updatedStatus);
                    updateSyncScheduleIfNecessary(updatedStatus, sdx, context);
                }
            }
        });
    }

    private Optional<SdxCluster> getCluster() {
        return sdxClusterRepository.findById(Long.valueOf(getLocalId()));
    }

    private DatalakeStatusEnum updateStatusIfNecessary(StackStatusV4Response stack, SdxCluster sdx, SdxStatusEntity sdxStatus) {
        if (isAvailable(stack)) {
            return updateToRunning(sdx, sdxStatus);
        } else if (isStopped(stack)) {
            return updateToStopped(sdx, sdxStatus);
        } else if (isUnreachable(stack)) {
            return updateToUnreachable(stack, sdx, sdxStatus);
        } else if (isNodeFailure(stack)) {
            return updateToNodeFailure(stack, sdx, sdxStatus);
        } else if (isDeleteCompleted(stack)) {
            return updateToStackDeleted(sdx, sdxStatus);
        } else if (isDeleteFailed(stack)) {
            return updateToDeleteFailed(sdx, sdxStatus, stack.getStatusReason());
        } else if (isDeletedOnProviderSide(stack)) {
            return updateToDeletedOnProviderSide(sdx, sdxStatus);
        } else if (isStale(stack)) {
            return updateToStale(sdx, sdxStatus, stack.getStatusReason());
        } else {
            LOGGER.debug("Sdx StatusChecker Job will ignore stack status {}. Current data lake state is {}.", stack, sdxStatus);
            return sdxStatus.getStatus();
        }
    }

    private void updateCertExpirationStateIfDifferent(SdxCluster sdx, StackStatusV4Response stack) {
        if (sdx.getCertExpirationState() != stack.getCertExpirationState()) {
            LOGGER.info("Updating CertExpirationState from [{}] to [{}] with details [{}]",
                    sdx.getCertExpirationState(), stack.getCertExpirationState(), stack.getCertExpirationDetails());
            sdxClusterRepository.updateCertExpirationState(sdx.getId(), stack.getCertExpirationState(), stack.getCertExpirationDetails());
        }
    }

    private void updateProviderSyncStateIfDifferent(SdxCluster sdx, StackStatusV4Response stack) {
        if (!CollectionUtils.isEqualCollection(
                CollectionUtils.emptyIfNull(sdx.getProviderSyncStates()),
                CollectionUtils.emptyIfNull(stack.getProviderSyncStates()))) {
            LOGGER.info("Updating ProviderSyncStates from [{}] to [{}]", sdx.getProviderSyncStates(), stack.getProviderSyncStates());
            sdxClusterRepository.updateProviderSyncStates(sdx.getId(), stack.getProviderSyncStates());
        }
    }

    private DatalakeStatusEnum updateToRunning(SdxCluster sdx, SdxStatusEntity sdxStatus) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.RUNNING;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), "", sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToStopped(SdxCluster sdx, SdxStatusEntity sdxStatus) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.STOPPED;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToUnreachable(StackStatusV4Response stackStatus, SdxCluster sdx, SdxStatusEntity sdxStatus) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.CLUSTER_UNREACHABLE;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            String statusReason = stackStatus.getStatus() == Status.UNREACHABLE ? stackStatus.getStatusReason() : stackStatus.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToNodeFailure(StackStatusV4Response stackStatus, SdxCluster sdx, SdxStatusEntity sdxStatus) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.NODE_FAILURE;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            String statusReason = stackStatus.getStatus() == Status.NODE_FAILURE ? stackStatus.getStatusReason() : stackStatus.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToStackDeleted(SdxCluster sdx, SdxStatusEntity sdxStatus) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.STACK_DELETED;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, "", sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToDeleteFailed(SdxCluster sdx, SdxStatusEntity sdxStatus, String stackStatusReason) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.DELETE_FAILED;
        if (!resultStatus.equals(sdxStatus.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.SDX_CLUSTER_DELETION_FAILED, stackStatusReason, sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToDeletedOnProviderSide(SdxCluster sdx, SdxStatusEntity status) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE;
        if (!resultStatus.equals(status.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.SDX_CLUSTER_DELETED_ON_PROVIDER_SIDE, "", sdx);
        }
        return resultStatus;
    }

    private DatalakeStatusEnum updateToStale(SdxCluster sdx, SdxStatusEntity status, String statusReason) {
        DatalakeStatusEnum resultStatus = DatalakeStatusEnum.STALE;
        if (!resultStatus.equals(status.getStatus())) {
            sdxStatusService.setStatusForDatalakeAndNotify(resultStatus, ResourceEvent.DATALAKE_STALE_STATUS, statusReason, sdx);
        }
        return resultStatus;
    }

    private boolean isAvailable(StackStatusV4Response stack) {
        return stack.getStatus().isAvailable() && stack.getClusterStatus() != null && stack.getClusterStatus().isAvailable();
    }

    private boolean isStopped(StackStatusV4Response stack) {
        return stack.getStatus().isStopped();
    }

    private boolean isUnreachable(StackStatusV4Response stack) {
        Status stackStatus = stack.getStatus();
        Status clusterStatus = stack.getClusterStatus();
        return stackStatus == Status.UNREACHABLE || stackStatus == Status.AVAILABLE && clusterStatus == Status.UNREACHABLE;
    }

    private boolean isNodeFailure(StackStatusV4Response stack) {
        Status stackStatus = stack.getStatus();
        Status clusterStatus = stack.getClusterStatus();
        return stackStatus == Status.NODE_FAILURE || stackStatus == Status.AVAILABLE && clusterStatus == Status.NODE_FAILURE;
    }

    private boolean isDeleteCompleted(StackStatusV4Response stack) {
        return Status.DELETE_COMPLETED.equals(stack.getStatus());
    }

    private boolean isDeleteFailed(StackStatusV4Response stack) {
        return Status.DELETE_FAILED.equals(stack.getStatus());
    }

    private boolean isDeletedOnProviderSide(StackStatusV4Response stack) {
        return Status.DELETED_ON_PROVIDER_SIDE.equals(stack.getStatus());
    }

    private boolean isStale(StackStatusV4Response stack) {
        return Status.STALE.equals(stack.getStatus());
    }

    private void logStateChange(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        LOGGER.info("Sdx StatusChecker job changed the status of datalake: '{}', from: '{}', to: '{}'", getLocalId(), from.name(), to.name());
    }

    private void updateSyncScheduleIfNecessary(DatalakeStatusEnum updatedStatus, SdxCluster sdx, JobExecutionContext context) {
        if (DatalakeStatusEnum.STACK_DELETED.equals(updatedStatus)) {
            LOGGER.info("Sdx status is {}. Unscheduling sdx sync job.", updatedStatus);
            unscheduleSync(sdx);
        } else if (DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE.equals(updatedStatus)) {
            if (!isLongSyncJob(context)) {
                LOGGER.info("Sdx status is {}. Rescheduling sync job to long interval.", updatedStatus);
                jobService.scheduleLongIntervalCheck(sdx.getId(), SdxClusterJobAdapter.class);
            }
        } else {
            if (isLongSyncJob(context)) {
                LOGGER.info("Sdx status is {}. Rescheduling sync job to short interval.", updatedStatus);
                jobService.schedule(sdx.getId(), SdxClusterJobAdapter.class);
            }
        }
    }

    private boolean isLongSyncJob(JobExecutionContext context) {
        return StatusCheckerJobService.LONG_SYNC_JOB_TYPE.equals(context.getMergedJobDataMap().get(StatusCheckerJobService.SYNC_JOB_TYPE));
    }

    private void unscheduleSync(SdxCluster sdx) {
        jobService.unschedule(String.valueOf(sdx.getId()));
    }

}
