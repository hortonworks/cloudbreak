package com.sequenceiq.datalake.job;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CLUSTER_AMBIGUOUS;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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

import io.opentracing.Tracer;

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
    private StatusCheckerJobService jobService;

    public SdxClusterStatusCheckerJob(Tracer tracer) {
        super(tracer, "SDX Cluster Status Checker");
    }

    @Override
    protected SdxCluster getMdcContextObject() {
        return sdxClusterRepository.findById(Long.valueOf(getLocalId())).orElse(null);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        LOGGER.debug("Sdx StatusChecker Job is running for datalake: '{}'", getLocalId());
        if (unschedulable()) {
            jobService.unschedule(getLocalId());
            LOGGER.debug("Sdx StatusChecker Job is unscheduled for datalake: '{}'", getLocalId());
            return;
        }
        handleSdxStatusChange();
    }

    //It is hard to refactor something simpler than a switch case
    //CHECKSTYLE:OFF: checkstyle:CyclomaticComplexity
    private void handleSdxStatusChange() {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(Long.valueOf(getLocalId()));
        cluster.ifPresent(sdx -> {
            StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(getRemoteResourceCrn());
            updateCertExpirationStateIfDifferent(sdx, stack);
            SdxStatusEntity status = sdxStatusService.getActualStatusForSdx(sdx);
            switch (status.getStatus()) {
                case RUNNING:
                    handleRunningSdx(stack, sdx);
                    break;
                case STOPPED:
                    handleStoppedSdx(stack, sdx);
                    break;
                case CLUSTER_AMBIGUOUS:
                    setAmbigousToUnreachableOrNodeFailure(stack, sdx);
                    break;
                case CLUSTER_UNREACHABLE:
                case NODE_FAILURE:
                    switchBackToAvailableIfClusterIsAvailable(stack, sdx, status.getStatus());
                    break;
                case SYNC_FAILED:
                case DATALAKE_UPGRADE_FAILED:
                case START_FAILED:
                case STOP_FAILED:
                case REPAIR_FAILED:
                    handleFailedSdx(stack, sdx);
                    break;
                case DELETED_ON_PROVIDER_SIDE:
                    handleDeletedOnProviderSideSdx(sdx);
                    break;
                default:
                    LOGGER.debug("Sdx StatusChecker Job will ignore state '{}' for datalake: '{}'", status.getStatus(), getLocalId());
            }
        });
    }
    //CHECKSTYLE:ON: checkstyle:CyclomaticComplexity

    private void updateCertExpirationStateIfDifferent(SdxCluster sdx, StackStatusV4Response stack) {
        if (sdx.getCertExpirationState() != stack.getCertExpirationState()) {
            LOGGER.info("Updating CertExpirationState from [{}] to [{}]", sdx.getCertExpirationState(), stack.getCertExpirationState());
            sdxClusterRepository.updateCertExpirationState(sdx.getId(), stack.getCertExpirationState());
        }
    }

    private boolean unschedulable() {
        return getRemoteResourceCrn() == null;
    }

    private void switchBackToAvailableIfClusterIsAvailable(StackStatusV4Response stack, SdxCluster sdx, DatalakeStatusEnum originalState) {
        if (stack.getStatus() == Status.AVAILABLE && stack.getClusterStatus() == Status.AVAILABLE) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, originalState);
        }
    }

    private void handleStoppedSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().isAvailable() && isClusterAvailable(stack)) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_START_FINISHED, "", sdx);
            logStateChange(DatalakeStatusEnum.STOPPED, DatalakeStatusEnum.RUNNING);
        } else if (stack.getStatus() == Status.DELETE_COMPLETED) {
            setDeleteCompleted(stack, sdx);
        } else if (stack.getStatus() == Status.DELETE_FAILED) {
            setDeleteFailed(stack, sdx);
        }
    }

    private void handleDeletedOnProviderSideSdx(SdxCluster sdx) {
        jobService.unschedule(String.valueOf(sdx.getId()));
    }

    private void handleFailedSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().isAvailable() && stack.getClusterStatus().isAvailable()) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_REPAIR_FINISHED, "", sdx);
            logStateChange(DatalakeStatusEnum.STOPPED, DatalakeStatusEnum.RUNNING);
        } else if (stack.getStatus() == Status.DELETE_COMPLETED) {
            setDeleteCompleted(stack, sdx);
        } else if (stack.getStatus() == Status.DELETE_FAILED) {
            setDeleteFailed(stack, sdx);
        }
    }

    private boolean isClusterAvailable(StackStatusV4Response stack) {
        return stack.getClusterStatus() != null && stack.getClusterStatus().isAvailable();
    }

    private void setDeleteFailed(StackStatusV4Response stack, SdxCluster sdx) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_FAILED, ResourceEvent.SDX_CLUSTER_DELETION_FAILED, stack.getStatusReason(),
                sdx);
        logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.DELETE_FAILED);
    }

    private void setDeletedOnProviderSide(StackStatusV4Response stack, SdxCluster sdx) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE, ResourceEvent.SDX_CLUSTER_DELETED_ON_PROVIDER_SIDE, "", sdx);
        logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.DELETED_ON_PROVIDER_SIDE);
    }

    private void setDeleteCompleted(StackStatusV4Response stack, SdxCluster sdx) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, "", sdx);
        jobService.unschedule(String.valueOf(sdx.getId()));
        logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STACK_DELETED);
    }

    private void handleRunningSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().isStopped()) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STOPPED);
        } else if (stack.getStatus() == Status.DELETE_COMPLETED) {
            setDeleteCompleted(stack, sdx);
        } else if (stack.getStatus() == Status.DELETE_FAILED) {
            setDeleteFailed(stack, sdx);
        } else if (stack.getStatus() == Status.DELETED_ON_PROVIDER_SIDE) {
            setDeletedOnProviderSide(stack, sdx);
        } else if (isUnreachable(stack.getStatus(), stack.getClusterStatus())) {
            String statusReason = stack.getStatus() == Status.UNREACHABLE ? stack.getStatusReason() : stack.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CLUSTER_UNREACHABLE, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.CLUSTER_UNREACHABLE);
        } else if (isNodeFailure(stack.getStatus(), stack.getClusterStatus())) {
            String statusReason = stack.getStatus() == Status.NODE_FAILURE ? stack.getStatusReason() : stack.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.NODE_FAILURE, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.NODE_FAILURE);
        }
    }

    private void setAmbigousToUnreachableOrNodeFailure(StackStatusV4Response stack, SdxCluster sdx) {
        if (isNodeFailure(stack.getStatus(), stack.getClusterStatus())) {
            String statusReason = stack.getStatus() == Status.NODE_FAILURE ? stack.getStatusReason() : stack.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.NODE_FAILURE, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
            logStateChange(CLUSTER_AMBIGUOUS, DatalakeStatusEnum.NODE_FAILURE);
        } else {
            String statusReason = stack.getStatus() == Status.UNREACHABLE ? stack.getStatusReason() : stack.getClusterStatusReason();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CLUSTER_UNREACHABLE, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    Collections.singleton(sdx.getClusterName()), statusReason, sdx);
            logStateChange(CLUSTER_AMBIGUOUS, DatalakeStatusEnum.CLUSTER_UNREACHABLE);
        }
    }

    private boolean isUnreachable(Status stackStatus, Status clusterStatus) {
        return stackStatus == Status.UNREACHABLE || (stackStatus == Status.AVAILABLE && clusterStatus == Status.UNREACHABLE);
    }

    private boolean isNodeFailure(Status stackStatus, Status clusterStatus) {
        return stackStatus == Status.NODE_FAILURE || (stackStatus == Status.AVAILABLE && clusterStatus == Status.NODE_FAILURE);
    }

    private void logStateChange(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        LOGGER.info("Sdx StatusChecker job changed the status of datalake: '{}', from: '{}', to: '{}'", getLocalId(), from.name(), to.name());
    }

}
