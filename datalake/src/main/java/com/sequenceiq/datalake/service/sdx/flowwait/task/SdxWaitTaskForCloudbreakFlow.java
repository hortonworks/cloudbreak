package com.sequenceiq.datalake.service.sdx.flowwait.task;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;

/**
 * This wait task will wait for a cloudbreak flow or flow chain to finish, then checks the result of the flow.
 */
public class SdxWaitTaskForCloudbreakFlow extends SdxWaitTask<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxWaitTaskForCloudbreakFlow.class);

    private final CloudbreakFlowService cloudbreakFlowService;

    public SdxWaitTaskForCloudbreakFlow(CloudbreakFlowService cloudbreakFlowService, SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage) {
        super(sdxCluster, pollingConfig, pollingMessage);
        this.cloudbreakFlowService = cloudbreakFlowService;
    }

    @Override
    public AttemptResult<Boolean> process() throws Exception {
        LOGGER.debug("{} polling cloudbreak for flow status: '{}' in '{}' env", getPollingMessage(), getSdxCluster().getClusterName(),
                getSdxCluster().getEnvName());
        FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(getSdxCluster());
        LOGGER.info("{} current cloudbreak flow state is: '{}'", getPollingMessage(), flowState);
        switch (flowState) {
            case RUNNING:
                LOGGER.debug("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", getPollingMessage(), getSdxCluster().getId());
                return AttemptResults.justContinue();
            case FINISHED:
                LOGGER.info("Polling flow {} has finished and flow was found to have succeeded", getPollingMessage());
                return AttemptResults.finishWith(Boolean.TRUE);
            case FAILED:
                String failedMessage = String.format("Flow %s did fail in cloudbreak", getPollingMessage());
                LOGGER.info(failedMessage);
                return AttemptResults.breakFor(failedMessage);
            case UNKNOWN:
            default:
                String message = String.format("Flow %s was either not found or the cluster has no flows, or some error happened.", getPollingMessage());
                LOGGER.info(message);
                return AttemptResults.breakFor(message);
        }
    }

}
