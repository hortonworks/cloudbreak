package com.sequenceiq.datalake.job;

import java.util.Optional;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.statuschecker.service.JobService;

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
    private JobService jobService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LOGGER.debug("Sdx StatusChecker Job is running for datalake: '{}'", getLocalId());
        StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(getRemoteResourceCrn());
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(Long.valueOf(getLocalId()));
        cluster.ifPresent(sdx -> {
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
                case UPGRADE_FAILED:
                case START_FAILED:
                case STOP_FAILED:
                case REPAIR_FAILED:
                    handleFailedSdx(stack, sdx);
                    break;
                default:
                    LOGGER.debug("Sdx StatusChecker Job will ignore state '{}' for datalake: '{}'", status.getStatus(), getLocalId());
            }
        });
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
