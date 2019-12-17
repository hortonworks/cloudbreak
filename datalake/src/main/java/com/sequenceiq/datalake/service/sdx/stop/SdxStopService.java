package com.sequenceiq.datalake.service.sdx.stop;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.FreeipaService;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.DistroxService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class SdxStopService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private DistroxService distroxService;

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public void triggerStopIfClusterNotStopped(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        checkFreeipaRunning(cluster.getEnvCrn());
        sdxReactorFlowManager.triggerSdxStopFlow(cluster.getId());
    }

    public void stop(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        try {
            LOGGER.info("Triggering stop flow for cluster {}", sdxCluster.getClusterName());
            stackV4Endpoint.putStop(0L, sdxCluster.getClusterName());
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.STOP_IN_PROGRESS, ResourceEvent.SDX_STOP_STARTED, "Datalake stop in progress",
                    sdxCluster);
            cloudbreakFlowService.getAndSaveLastCloudbreakFlowChainId(sdxCluster);
        } catch (NotFoundException e) {
            LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
        } catch (ClientErrorException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not stop stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Can not stop stack, client error happened on Cloudbreak side: " + errorMessage);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Can not stop stack {} from cloudbreak: {}", sdxCluster.getStackId(), errorMessage, e);
            throw new RuntimeException("Can not stop stack, web application error happened on Cloudbreak side: " + errorMessage);
        }
    }

    public void waitCloudbreakCluster(Long sdxId, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> checkClusterStatusDuringStop(sdxCluster));
    }

    protected AttemptResult<StackV4Response> checkClusterStatusDuringStop(SdxCluster sdxCluster) throws JsonProcessingException {
        LOGGER.info("Stop polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("Stop polling cancelled in inmemory store, id: " + sdxCluster.getId());
                return AttemptResults.breakFor("Stop polling cancelled in inmemory store, id: " + sdxCluster.getId());
            } else if (cloudbreakFlowService.isLastKnownFlowRunning(sdxCluster)) {
                LOGGER.info("Stop polling will continue, cluster has an active flow in Cloudbreak, id: {}", sdxCluster.getId());
                return AttemptResults.justContinue();
            } else {
                return getStackResponseAttemptResult(sdxCluster);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster) throws JsonProcessingException {
        StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (stackAndClusterStopped(stackV4Response, cluster)) {
            return AttemptResults.finishWith(stackV4Response);
        } else {
            if (Status.STOP_FAILED.equals(stackV4Response.getStatus())) {
                LOGGER.info("Stack stop failed for Stack {} with status {}, reason {}", stackV4Response.getName(), stackV4Response.getStatus(),
                        stackV4Response.getStatusReason());
                return sdxStopFailed(sdxCluster, stackV4Response.getStatusReason());
            } else if (cluster != null && Status.STOP_FAILED.equals(cluster.getStatus())) {
                LOGGER.info("Cluster stop failed for Cluster {} status {}, reason {}", cluster.getName(),
                        stackV4Response.getCluster().getStatus(), stackV4Response.getStatusReason());
                return sdxStopFailed(sdxCluster, cluster.getStatusReason());
            } else if (!stackV4Response.getStatus().isStopState()) {
                return AttemptResults.breakFor("SDX stop failed '" + sdxCluster.getClusterName() + "', stack is in inconsistency state: "
                        + stackV4Response.getStatus());
            } else if (cluster != null && !cluster.getStatus().isStopState()) {
                return AttemptResults.breakFor("SDX stop failed '" + sdxCluster.getClusterName() + "', cluster is in inconsistency state: "
                        + cluster.getStatus());
            } else {
                return AttemptResults.justContinue();
            }
        }
    }

    private AttemptResult<StackV4Response> sdxStopFailed(SdxCluster sdxCluster, String statusReason) {
        return AttemptResults.breakFor("SDX stop failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }

    private boolean stackAndClusterStopped(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isStopped()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isStopped();
    }

    public void stopAllDatahub(Long sdxId) {
        SdxCluster sdxCluster = sdxService.getById(sdxId);
        distroxService.stopAttachedDistrox(sdxCluster.getEnvCrn());
    }

    private void checkFreeipaRunning(String envCrn) {
        DescribeFreeIpaResponse freeipa = freeipaService.describe(envCrn);
        if (freeipa != null && !freeipa.getStatus().isAvailable()) {
            throw new BadRequestException("Freeipa should be in Available state but currently is " + freeipa.getStatus().name());
        }
    }
}
