package com.sequenceiq.cloudbreak.service;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.flowlog.FlowRetryUtil;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@Service
public class OperationRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationRetryService.class);

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterService clusterService;

    public void retry(Stack stack) {
        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdOrderByCreatedDesc(stack.getId());
        if (FlowRetryUtil.isFlowPending(flowLogs)) {
            LOGGER.info("Retry cannot be performed, because there is already an active flow. stackId: {}", stack.getId());
            throw new BadRequestException("Retry cannot be performed, because there is already an active flow.");
        }

        Cluster cluster = stack.getCluster();
        if (Status.CREATE_FAILED.equals(stack.getStatus())
                || (Status.AVAILABLE.equals(stack.getStatus()) && Status.CREATE_FAILED.equals(cluster.getStatus()))) {
            Optional<FlowLog> failedFlowLog = FlowRetryUtil.getMostRecentFailedLog(flowLogs);
            if (Status.CREATE_FAILED.equals(stack.getStatus())) {
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.RETRY);
            } else if (Status.CREATE_FAILED.equals(cluster.getStatus())) {
                clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_IN_PROGRESS);
            }
            failedFlowLog.map(log -> FlowRetryUtil.getLastSuccessfulStateLog(log.getCurrentState(), flowLogs))
                    .ifPresent(flow2Handler::restartFlow);
        } else {
            LOGGER.info("Retry can only be performed, if stack or cluster creation failed. stackId: {}", stack.getId());
            throw new BadRequestException("Retry can only be performed, if stack or cluster creation failed.");
        }
    }

}
