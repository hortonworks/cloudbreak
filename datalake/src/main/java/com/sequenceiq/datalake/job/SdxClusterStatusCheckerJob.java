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
        LOGGER.debug("StatusChecker Job is running for datalake: {}", getLocalId());
        StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(getRemoteResourceCrn());
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(Long.valueOf(getLocalId()));
        cluster.ifPresent(sdx -> {
            SdxStatusEntity status = sdxStatusService.getActualStatusForSdx(sdx);
            if (status.getStatus() == DatalakeStatusEnum.RUNNING) {
                handleRunningSdx(stack, sdx);
            }
            if (status.getStatus() == DatalakeStatusEnum.STOPPED) {
                handleStoppedSdx(stack, sdx);
            }
            if (status.getStatus() == DatalakeStatusEnum.CLUSTER_AMBIGUOUS) {
                handleAmbiguousSdx(stack, sdx);
            }
        });
    }

    private void handleAmbiguousSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().equals(Status.AVAILABLE) && stack.getClusterStatus() == Status.AVAILABLE) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED, "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STACK_DELETED);
        }
    }

    private void handleStoppedSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().isAvailable() && stack.getClusterStatus().isAvailable()) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
            logStateChange(DatalakeStatusEnum.STOPPED, DatalakeStatusEnum.RUNNING);
        }
    }

    private void handleRunningSdx(StackStatusV4Response stack, SdxCluster sdx) {
        if (stack.getStatus().isStopped()) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STOPPED);
        }
        if (stack.getStatus().equals(Status.DELETE_COMPLETED)) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, "", sdx);
            jobService.unschedule(String.valueOf(sdx.getId()));
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STACK_DELETED);
        }
        if (stack.getStatus().equals(Status.DELETE_FAILED)) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_FAILED, ResourceEvent.SDX_CLUSTER_DELETION_FAILED, "", sdx);
            jobService.unschedule(String.valueOf(sdx.getId()));
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.DELETE_FAILED);
        }
        if (stack.getStatus().equals(Status.AVAILABLE) && stack.getClusterStatus() == Status.AMBIGUOUS) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CLUSTER_AMBIGUOUS, ResourceEvent.CLUSTER_AMBARI_CLUSTER_SYNCHRONIZED,
                    "", sdx);
            logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STACK_DELETED);
        }
    }

    private void logStateChange(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        LOGGER.info("StatusChecker job changed the status of datalake: {}, from: {}, to: {}", getLocalId(), from.name(), to.name());
    }

}
