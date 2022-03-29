package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;

@Component
public class CDPRequestProcessingStepMapper {

    public UsageProto.CDPRequestProcessingStep.Value mapIt(FlowDetails flow) {
        UsageProto.CDPRequestProcessingStep.Value requestType = UsageProto.CDPRequestProcessingStep.Value.UNSET;
        if (flow != null) {
            if (isFirstStep(flow)) {
                requestType = UsageProto.CDPRequestProcessingStep.Value.INIT;
            } else if (isLastStep(flow)) {
                requestType = UsageProto.CDPRequestProcessingStep.Value.FINAL;
            }
        }
        return requestType;
    }

    public boolean isFirstStep(FlowDetails flow) {
        return "INIT_STATE".equals(flow.getNextFlowState());
    }

    public boolean isLastStep(FlowDetails flow) {
        return flow.getNextFlowState() != null
                && (flow.getNextFlowState().endsWith("_FAILED_STATE")
                || flow.getNextFlowState().endsWith("_FAIL_STATE")
                || flow.getNextFlowState().endsWith("_FINISHED_STATE"));
    }
}
