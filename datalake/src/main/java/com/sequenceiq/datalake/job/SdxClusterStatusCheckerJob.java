package com.sequenceiq.datalake.job;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;

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

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LOGGER.debug("Sdx StatusChecker Job is running for datalake: '{}'", getLocalId());
        if (unschedulable()) {
            jobService.unschedule(getLocalId());
            LOGGER.debug("Sdx StatusChecker Job is unscheduled for datalake: '{}'", getLocalId());
            return;
        }
        handleSdxStatusChange();
        MDCBuilder.cleanupMdc();
    }

    private void handleSdxStatusChange() {
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(Long.valueOf(getLocalId()));
        cluster.ifPresent(sdx -> {
            buildMdcContext(sdx);
            StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(getRemoteResourceCrn());
            SdxStatusEntity status = sdxStatusService.getActualStatusForSdx(sdx);
            switch (status.getStatus()) {
                case RUNNING:
                    handleRunningSdx(stack, sdx);
                    break;
                case STOPPED:
                    handleStoppedSdx(stack, sdx);
                    break;
                case CLUSTER_AMBIGUOUS:
                    handleAmbiguousSdx(stack, sdx);
                    break;
                case SYNC_FAILED:
                case DATALAKE_UPGRADE_FAILED:
                case START_FAILED:
                case STOP_FAILED:
                case REPAIR_FAILED:
                    handleFailedSdx(stack, sdx);
                    break;
                case DELETED_ON_PROVIDER_SIDE:
                    handleDeletedOnProviderSideSdx(stack, sdx);
                    break;
                default:
                    LOGGER.debug("Sdx StatusChecker Job will ignore state '{}' for datalake: '{}'", status.getStatus(), getLocalId());
            }
        });
    }

    private boolean unschedulable() {
        return getRemoteResourceCrn() == null;
    }

    private void buildMdcContext(SdxCluster sdx) {
        MDCBuilder.buildMdcContext(sdx);
        MDCBuilder.addRequestId(UUID.randomUUID().toString());
    }

    private void handleAmbiguousSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus() == Status.AVAILABLE && stack.getClusterStatus() == Status.AVAILABLE) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED, "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
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

    private void handleDeletedOnProviderSideSdx(StackStatusV4Response stack, SdxCluster sdx) {
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
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_FAILED, ResourceEvent.SDX_CLUSTER_DELETION_FAILED, "", sdx);
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
        } else if (stack.getStatus() == Status.AVAILABLE && stack.getClusterStatus() == Status.AMBIGUOUS) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CLUSTER_AMBIGUOUS, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.CLUSTER_AMBIGUOUS);
        }
    }

    private void logStateChange(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        LOGGER.info("Sdx StatusChecker job changed the status of datalake: '{}', from: '{}', to: '{}'", getLocalId(), from.name(), to.name());
    }

}
