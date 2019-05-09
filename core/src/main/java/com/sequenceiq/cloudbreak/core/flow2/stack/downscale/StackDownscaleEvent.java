package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum StackDownscaleEvent implements FlowEvent {
    STACK_DOWNSCALE_EVENT("STACK_DOWNSCALE_TRIGGER_EVENT"),
    DOWNSCALE_RESOURCES_COLLECTED_EVENT(CloudPlatformResult.selector(DownscaleStackCollectResourcesResult.class)),
    DOWNSCALE_RESOURCES_FAILURE_EVENT(CloudPlatformResult.failureSelector(DownscaleStackCollectResourcesResult.class)),
    DOWNSCALE_FINISHED_EVENT(CloudPlatformResult.selector(DownscaleStackResult.class)),
    DOWNSCALE_FAILURE_EVENT(CloudPlatformResult.failureSelector(DownscaleStackResult.class)),
    DOWNSCALE_FINALIZED_EVENT("DOWNSCALESTACKFINALIZED"),
    DOWNSCALE_FAIL_HANDLED_EVENT("DOWNSCALEFAILHANDLED");

    private final String event;

    StackDownscaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
