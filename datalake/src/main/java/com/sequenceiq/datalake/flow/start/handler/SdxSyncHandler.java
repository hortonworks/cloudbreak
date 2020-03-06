package com.sequenceiq.datalake.flow.start.handler;

import static com.sequenceiq.datalake.flow.start.handler.SdxStartWaitHandler.DURATION_IN_MINUTES;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.start.event.SdxSyncSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxSyncWaitRequest;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class SdxSyncHandler extends ExceptionCatcherEventHandler<SdxSyncWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSyncHandler.class);

    private static final int SLEEP_TIME = 5;

    private static final int ATTEMPT = 40;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public String selector() {
        return "SdxSyncWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxStartFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        SdxSyncWaitRequest waitRequest = event.getData();
        Long sdxId = waitRequest.getResourceId();
        String userId = waitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Polling stack sync process for id: {}", sdxId);
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            FlowIdentifier flowIdentifier = sdxService.sync(sdxCluster.getClusterName());
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
            StackV4Response stackV4Response = pollingSync(sdxCluster);
            updateSdxStatus(sdxCluster, stackV4Response);
            response = new SdxSyncSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Sync polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStartFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Sync poller stopped for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake sync timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Sync polling failed for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }

    private StackV4Response pollingSync(SdxCluster sdxCluster) {
        return Polling.waitPeriodly(SLEEP_TIME, TimeUnit.SECONDS)
                .stopIfException(true)
                .stopAfterAttempt(ATTEMPT)
                .run(() -> checkSyncStatus(sdxCluster));
    }

    private AttemptResult<StackV4Response> checkSyncStatus(SdxCluster sdxCluster) {
        LOGGER.info("Sync polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("Sync polling cancelled in inmemory store, id: " + sdxCluster.getId());
                return AttemptResults.breakFor("Sync polling cancelled in inmemory store, id: " + sdxCluster.getId());
            }
            if (cloudbreakFlowService.isLastKnownFlowRunning(sdxCluster)) {
                LOGGER.info("Sync polling will continue, cluster has an active flow in Cloudbreak, id: " + sdxCluster.getId());
                return AttemptResults.justContinue();
            } else {
                StackV4Response stackV4Response = stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                return AttemptResults.finishWith(stackV4Response);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private void updateSdxStatus(SdxCluster cluster, StackV4Response stack) {
        DatalakeStatusEnum status;
        String reason;
        Status stackStatus = stack.getStatus();
        Status clusterStatus = stack.getCluster().getStatus();

        if (stackStatus == Status.AVAILABLE || clusterStatus == Status.AVAILABLE) {
            status = DatalakeStatusEnum.RUNNING;
            reason = stack.getStatusReason();
        } else if (Status.STOPPED == stackStatus && Status.STOPPED == clusterStatus) {
            status = DatalakeStatusEnum.STOPPED;
            reason = stack.getStatusReason();
        } else if (Status.STOP_FAILED == stackStatus) {
            status = DatalakeStatusEnum.STOP_FAILED;
            reason = stack.getStatusReason();
        } else if (Status.STOP_FAILED == clusterStatus) {
            status = DatalakeStatusEnum.STOP_FAILED;
            reason = stack.getCluster().getStatusReason();
        } else if (Status.START_FAILED == stackStatus) {
            status = DatalakeStatusEnum.START_FAILED;
            reason = stack.getStatusReason();
        } else if (Status.START_FAILED == clusterStatus) {
            status = DatalakeStatusEnum.START_FAILED;
            reason = stack.getCluster().getStatusReason();
        } else {
            LOGGER.info("Unknown stack or cluster status (stack: {}, cluster: {}), so set to SYNC_FAILED", stackStatus, clusterStatus);
            status = DatalakeStatusEnum.SYNC_FAILED;
            reason = String.format("Unknown stack or cluster status (stack: %s, cluster: %s), so set to SYNC_FAILED", stackStatus, clusterStatus);
        }
        sdxStatusService.setStatusForDatalakeAndNotify(status, ResourceEvent.SDX_SYNC_FAILED, reason, cluster);
    }
}
