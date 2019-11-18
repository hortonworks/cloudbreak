package com.sequenceiq.datalake.job;

import java.util.Optional;

import javax.inject.Inject;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxJobService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component
public class DatalakeStatusCheckerJob extends QuartzJobBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeStatusCheckerJob.class);

    private String sdxId;

    private String stackCrn;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxJobService sdxJobService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        LOGGER.debug("StatusChecker Job is running for datalake: {}", sdxId);
        StackStatusV4Response stack = cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().getStatusByCrn(stackCrn);
        Optional<SdxCluster> cluster = sdxClusterRepository.findById(Long.valueOf(sdxId));
        cluster.ifPresent(sdx -> {
            SdxStatusEntity status = sdxStatusService.getActualStatusForSdx(sdx);
            if (status.getStatus() == DatalakeStatusEnum.RUNNING && stack.getStatus().isStopped()) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOPPED, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
                logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STOPPED);
            }
            if (status.getStatus() == DatalakeStatusEnum.RUNNING && stack.getStatus().equals(Status.DELETE_COMPLETED)) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STACK_DELETED, ResourceEvent.SDX_CLUSTER_DELETION_FINISHED, "", sdx);
                sdxJobService.unschedule(sdx.getId());
                logStateChange(DatalakeStatusEnum.RUNNING, DatalakeStatusEnum.STACK_DELETED);
            }
            if (status.getStatus() == DatalakeStatusEnum.STOPPED && stack.getStatus().isAvailable() && stack.getClusterStatus().isAvailable()) {
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SDX_STOP_FINISHED, "", sdx);
                logStateChange(DatalakeStatusEnum.STOPPED, DatalakeStatusEnum.RUNNING);
            }
        });
    }

    private void logStateChange(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        LOGGER.info("StatusChecker job changed the status of datalake: {}, from: {}, to: {}", sdxId, from.name(), to.name());

    }

    public String getStackCrn() {
        return stackCrn;
    }

    public void setStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
    }

    public String getSdxId() {
        return sdxId;
    }

    public void setSdxId(String sdxId) {
        this.sdxId = sdxId;
    }
}
