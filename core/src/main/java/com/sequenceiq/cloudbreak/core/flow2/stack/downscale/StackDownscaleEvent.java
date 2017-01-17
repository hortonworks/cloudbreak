package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;

public enum StackDownscaleEvent implements FlowEvent {
    DOWNSCALE_EVENT(FlowTriggers.STACK_DOWNSCALE_TRIGGER_EVENT),
    DOWNSCALE_RESOURCES_COLLECTED_EVENT(DownscaleStackResult.selector(DownscaleStackCollectResourcesResult.class)),
    DOWNSCALE_RESOURCES_FAILURE_EVENT(DownscaleStackResult.failureSelector(DownscaleStackCollectResourcesResult.class)),
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
