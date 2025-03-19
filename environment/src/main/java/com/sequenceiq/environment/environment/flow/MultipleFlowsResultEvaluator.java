package com.sequenceiq.environment.environment.flow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class MultipleFlowsResultEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFlowsResultEvaluator.class);

    private final FlowEndpoint flowEndpoint;

    public MultipleFlowsResultEvaluator(FlowEndpoint flowEndpoint) {
        this.flowEndpoint = flowEndpoint;
    }

    public boolean anyFailed(List<FlowIdentifier> flowIds) {
        return flowIds.stream().anyMatch(this::isFlowNotFinishedOrFailed);
    }

    private Boolean isFlowNotFinishedOrFailed(FlowIdentifier flowId) {
        FlowCheckResponse flowCheckResponse;
        switch (flowId.getType()) {
            case FLOW:
                flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> flowEndpoint.hasFlowRunningByFlowId(flowId.getPollableId()));
                break;
            case FLOW_CHAIN:
                flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> flowEndpoint.hasFlowRunningByChainId(flowId.getPollableId()));
                break;
            case NOT_TRIGGERED:
                LOGGER.warn("Flow {} is not triggered for DataHub Upgrade CCM", flowId);
                return true;
            default:
                throw new IllegalStateException("Unexpected Flow type: " + flowId.getType());
        }
        if (flowCheckResponse.getLatestFlowFinalizedAndFailed()) {
            LOGGER.debug("Flow {} is failed for DataHub Upgrade CCM", flowId);
            return true;
        }
        LOGGER.debug("Flow {} is successful for DataHub Upgrade CCM", flowId);
        return flowCheckResponse.getHasActiveFlow();
    }
}
