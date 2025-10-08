package com.sequenceiq.datalake.job.salt;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatusResponse;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SaltService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.FlowLogService;

@DisallowConcurrentExecution
@Component
public class SdxSaltStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSaltStatusCheckerJob.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private SdxSaltStatusCheckerJobService jobService;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private SaltService saltService;

    @Override
    protected Optional<Object> getMdcContextObject() {
        return getCluster().map(Object.class::cast);
    }

    private Optional<SdxCluster> getCluster() {
        return sdxClusterRepository.findById(Long.valueOf(getLocalId()));
    }

    @Override
    protected void executeJob(JobExecutionContext context) {
        if (getLocalId() != null && flowLogService.isOtherFlowRunning(Long.valueOf(getLocalId()))) {
            LOGGER.debug("SdxSaltStatusCheckerJob cannot run, because flow is running for datalake: {}", getLocalId());
            return;
        }
        LOGGER.debug("SdxSaltStatusCheckerJob is running for datalake: '{}'", getLocalId());
        if (getRemoteResourceCrn() == null) {
            jobService.unschedule(context.getJobDetail().getKey());
            LOGGER.debug("SdxSaltStatusCheckerJob is unscheduled for datalake: '{}'", getLocalId());
        } else {
            checkSdxStack(context);
        }
    }

    private void checkSdxStack(JobExecutionContext context) {
        Optional<SdxCluster> sdxClusterOptional = getCluster();
        if (sdxClusterOptional.isPresent()) {
            SdxCluster sdxCluster = sdxClusterOptional.get();
            SdxStatusEntity sdxStatus = sdxStatusService.getActualStatusForSdx(sdxCluster);
            DatalakeStatusEnum status = sdxStatus.getStatus();
            if (status.isDeleteInProgressOrCompleted() || status.isProvisioningFailed() || status.isDeletedOnProviderSide()) {
                LOGGER.debug("SDX cluster with id {} status is {}, unscheduling salt status check", getLocalId(), status);
                jobService.unschedule(context.getJobDetail().getKey());
            } else if (status.isStopState()) {
                LOGGER.debug("SDX cluster with id {} is stopped, can not run salt status check", getLocalId());
            } else {
                StackV4Endpoint stackV4Endpoint = cloudbreakInternalCrnClient.withInternalCrn().stackV4Endpoint();
                SaltPasswordStatusResponse saltPasswordStatus = stackV4Endpoint.getSaltPasswordStatus(0L, getRemoteResourceCrn());
                Optional<RotateSaltPasswordReason> reasonOptional = RotateSaltPasswordReason.getForStatus(saltPasswordStatus.getStatus());
                if (reasonOptional.isPresent()) {
                    LOGGER.info("Salt password rotation is needed for SDX {} based on response {}", sdxCluster.getCrn(), saltPasswordStatus);
                    try {
                        saltService.rotateSaltPassword(sdxCluster);
                    } catch (WebApplicationException e) {
                        String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                        LOGGER.warn("Salt password rotation failed. {}", errorMessage);
                    }
                } else {
                    LOGGER.debug("Salt password rotation is NOT needed for SDX {} based on response {}", sdxCluster.getCrn(), saltPasswordStatus);
                }
            }
        } else {
            LOGGER.warn("SDX cluster with id {} not found, unscheduling salt status check", getLocalId());
            jobService.unschedule(context.getJobDetail().getKey());
        }
    }
}
