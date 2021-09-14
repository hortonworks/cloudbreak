package com.sequenceiq.datalake.service.sdx.flowwait;

import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.RUNNING;
import static com.sequenceiq.datalake.service.sdx.CloudbreakFlowService.FlowState.UNKNOWN;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.upgrade.SdxUpgradeValidationResultProvider;

@Component
@Scope("prototype")
public class SdxWaitTaskForCloudbreakFlow extends SdxWaitTask<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxWaitTaskForCloudbreakFlow.class);

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxUpgradeValidationResultProvider cloudbreakFlowResultProvider;

    public SdxWaitTaskForCloudbreakFlow(SdxCluster sdxCluster, PollingConfig pollingConfig, String pollingMessage, CloudbreakFlowService cloudbreakFlowService,
            SdxUpgradeValidationResultProvider sdxUpgradeValidationResultProvider) {
        super(sdxCluster, pollingConfig, pollingMessage);
        this.cloudbreakFlowService = cloudbreakFlowService;
        this.cloudbreakFlowResultProvider = sdxUpgradeValidationResultProvider;
    }

    @Override
    public AttemptResult<Boolean> process() throws Exception {
        LOGGER.info("{} polling cloudbreak for flow status: '{}' in '{}' env", getPollingMessage(), getSdxCluster().getClusterName(),
                getSdxCluster().getEnvName());
        try {
            CloudbreakFlowService.FlowState flowState = cloudbreakFlowService.getLastKnownFlowState(getSdxCluster());
            if (RUNNING.equals(flowState)) {
                LOGGER.info("{} polling will continue, cluster has an active flow in Cloudbreak, id: {}", getPollingMessage(), getSdxCluster().getId());
                return AttemptResults.justContinue();
            } else if (UNKNOWN.equals(flowState)) {
                String message = String.format("Flow %s was either not found or the cluster has no flow", getPollingMessage());
                LOGGER.info(message);
                return AttemptResults.breakFor(message);
            } else {
                return checkFlowResult(getSdxCluster());
            }
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.debug("Stack not found on CB side " + getSdxCluster().getClusterName(), e);
            return AttemptResults.breakFor("Stack not found on CB side " + getSdxCluster().getClusterName());
        }
    }

    private AttemptResult<Boolean> checkFlowResult(SdxCluster sdxCluster) {
        boolean flowResult = cloudbreakFlowService.getFlowResultByFlowId(sdxCluster);
        LOGGER.debug("Flow result is for flow {} is {}", getPollingMessage(), flowResult);
        return flowResult
                ? AttemptResults.finishWith(Boolean.TRUE)
                : AttemptResults.breakFor(String.format("Flow %s has a failed state", getPollingMessage()));
    }

}
