package com.sequenceiq.environment.environment.flow;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxFlowEndpoint;

@Service
public class DatalakeMultipleFlowsResultEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeMultipleFlowsResultEvaluator.class);

    private final SdxFlowEndpoint sdxFlowEndpoint;

    public DatalakeMultipleFlowsResultEvaluator(SdxFlowEndpoint sdxFlowEndpoint) {
        this.sdxFlowEndpoint = sdxFlowEndpoint;
    }

    public boolean allFinished(List<FlowIdentifier> flowIds) {
        return !flowIds.stream().anyMatch(this::isFlowRunning);
    }

    public boolean anyFailed(List<FlowIdentifier> flowIds) {
        return flowIds.stream().anyMatch(this::isFlowFailed);
    }

    private Optional<FlowCheckResponse> getFlowCheckResponse(FlowIdentifier flowId) {
        Optional<FlowCheckResponse> flowCheckResponse = Optional.empty();
        switch (flowId.getType()) {
            case FLOW:
                flowCheckResponse = Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> sdxFlowEndpoint.hasFlowRunningByFlowId(flowId.getPollableId())));
                break;
            case FLOW_CHAIN:
                flowCheckResponse = Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> sdxFlowEndpoint.hasFlowRunningByChainId(flowId.getPollableId())));
                break;
            case NOT_TRIGGERED:
                LOGGER.debug("Flow {} is not triggered.", flowId);
                break;
            default:
                throw new IllegalStateException("Unexpected Flow type: " + flowId.getType());
        }
        return flowCheckResponse;
    }

    private Boolean isFlowFailed(FlowIdentifier flowId) {
        Optional<FlowCheckResponse> flowCheckResponse = getFlowCheckResponse(flowId);
        if (flowCheckResponse.isPresent()) {
            return flowCheckResponse.get().getLatestFlowFinalizedAndFailed();
        } else {
            return Boolean.FALSE;
        }
    }

    private Boolean isFlowRunning(FlowIdentifier flowId) {
        Optional<FlowCheckResponse> flowCheckResponse = getFlowCheckResponse(flowId);
        if (flowCheckResponse.isPresent()) {
            return flowCheckResponse.get().getHasActiveFlow();
        } else {
            return Boolean.FALSE;
        }
    }

}

