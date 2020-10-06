package com.sequenceiq.datalake.service.sdx.cert;

import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.FINISHED;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;

import java.util.Collections;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState;
import com.sequenceiq.datalake.service.sdx.status.AvailabilityChecker;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Component
public class ClusterStatusCheckerForCertRotation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusCheckerForCertRotation.class);

    @Inject
    private AvailabilityChecker availabilityChecker;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxStatusService sdxStatusService;

    public AttemptResult<StackV4Response> checkClusterStatusDuringRotate(SdxCluster sdxCluster) throws JsonProcessingException {
        LOGGER.info("Certificate rotation polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
        try {
            if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                LOGGER.info("Certificate rotation polling cancelled in inmemory store, id: " + sdxCluster.getId());
                return AttemptResults.breakFor("Certificate rotation polling cancelled in inmemory store, id: " + sdxCluster.getId());
            }
            FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(sdxCluster);
            if (RUNNING.equals(flowState)) {
                LOGGER.info("Certificate rotation polling will continue, cluster has an active flow in Cloudbreak, id: " + sdxCluster.getId());
                return AttemptResults.justContinue();
            } else {
                return getStackResponseAttemptResult(sdxCluster, flowState);
            }
        } catch (NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
        }
    }

    private AttemptResult<StackV4Response> getStackResponseAttemptResult(SdxCluster sdxCluster, FlowState flowState) throws JsonProcessingException {
        StackV4Response stackV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.get(0L, sdxCluster.getClusterName(), Collections.emptySet(), sdxCluster.getAccountId()));
        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
        ClusterV4Response cluster = stackV4Response.getCluster();
        if (availabilityChecker.stackAndClusterAvailable(stackV4Response, cluster)) {
            return AttemptResults.finishWith(stackV4Response);
        } else {
            if (Status.UPDATE_FAILED.equals(stackV4Response.getStatus())) {
                LOGGER.info("Cert rotation failed for Stack {} with status {}", stackV4Response.getName(), stackV4Response.getStatus());
                return failSdxCertRotation(sdxCluster, stackV4Response.getStatusReason());
            } else if (Status.UPDATE_FAILED.equals(stackV4Response.getCluster().getStatus())) {
                LOGGER.info("Cert rotation failed for Cluster {} status {}", stackV4Response.getCluster().getName(),
                        stackV4Response.getCluster().getStatus());
                return failSdxCertRotation(sdxCluster, stackV4Response.getCluster().getStatusReason());
            } else {
                if (FINISHED.equals(flowState)) {
                    String message = sdxStatusService.getShortStatusMessage(stackV4Response);
                    LOGGER.info("Cert rotationn flow finished, but stack or cluster is not available. {}", message);
                    return failSdxCertRotation(sdxCluster, message);
                } else {
                    return AttemptResults.justContinue();
                }
            }
        }
    }

    private AttemptResult<StackV4Response> failSdxCertRotation(SdxCluster sdxCluster, String statusReason) {
        LOGGER.info("SDX ceert rotation failed: " + statusReason);
        return AttemptResults.breakFor("SDX cert rotation failed '" + sdxCluster.getClusterName() + "', " + statusReason);
    }
}
