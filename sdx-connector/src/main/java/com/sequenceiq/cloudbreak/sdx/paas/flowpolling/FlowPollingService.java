package com.sequenceiq.cloudbreak.sdx.paas.flowpolling;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxFlowEndpoint;

@Service
public class FlowPollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPollingService.class);

    private final SdxFlowEndpoint sdxFlowEndpoint;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FlowPollingService(SdxFlowEndpoint sdxFlowEndpoint,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.sdxFlowEndpoint = sdxFlowEndpoint;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public AttemptResult<Object> pollFlowIdAndReturnAttemptResult(FlowIdentifier flowId) {
        return getFlowCheckResponse(flowId)
                .map(flowCheckResponse -> {
                    if (Boolean.TRUE.equals(flowCheckResponse.getLatestFlowFinalizedAndFailed())) {
                        return AttemptResults.breakFor(new IllegalStateException("SDX flow operation failed for flow ID: " + flowId));
                    } else if (Boolean.TRUE.equals(flowCheckResponse.getHasActiveFlow())) {
                        return AttemptResults.justContinue();
                    }
                    return AttemptResults.finishWith(null);
                })
                .orElse(AttemptResults.finishWith(null));
    }

    private Optional<FlowCheckResponse> getFlowCheckResponse(FlowIdentifier flowId) {
        Optional<FlowCheckResponse> flowCheckResponse = Optional.empty();
        switch (flowId.getType()) {
            case FLOW:
                flowCheckResponse = Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> sdxFlowEndpoint.hasFlowRunningByFlowId(flowId.getPollableId())));
                break;
            case FLOW_CHAIN:
                flowCheckResponse = Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> sdxFlowEndpoint.hasFlowRunningByChainId(flowId.getPollableId())));
                break;
            case NOT_TRIGGERED:
                LOGGER.debug("Flow {} is not triggered. Most likely terminated.", flowId);
                break;
            default:
                throw new IllegalStateException("Unexpected Flow type: " + flowId.getType());
        }
        return flowCheckResponse;
    }
}
