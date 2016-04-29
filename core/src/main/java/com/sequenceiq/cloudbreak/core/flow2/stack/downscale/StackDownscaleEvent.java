package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackDownscaleEvent implements FlowEvent {
    DOWNSCALE_EVENT(FlowPhases.STACK_DOWNSCALE.name()),
    DOWNSCALE_FINISHED_EVENT(DownscaleStackResult.selector(DownscaleStackResult.class)),
    DOWNSCALE_FAILURE_EVENT(DownscaleStackResult.failureSelector(DownscaleStackResult.class)),
    DOWNSCALE_FINALIZED_EVENT("DOWNSCALESTACKFINALIZED"),
    DOWNSCALE_FAIL_HANDLED_EVENT("DOWNSCALEFAILHANDLED");

    private String stringRepresentation;

    StackDownscaleEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
