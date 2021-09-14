package com.sequenceiq.datalake.service.sdx.flowcheck;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.FlowCheckResponse;

@Component
public class FlowCheckResponseToFlowStateConverter {

    public FlowState convert(FlowCheckResponse flowCheckResponse) {
        if (flowCheckResponse.getHasActiveFlow()) {
            return FlowState.RUNNING;
        } else {
            if (flowCheckResponse.getLatestFlowFinalizedAndFailed() != null && flowCheckResponse.getLatestFlowFinalizedAndFailed()) {
                return FlowState.FAILED;
            } else {
                return FlowState.FINISHED;
            }
        }
    }

}
