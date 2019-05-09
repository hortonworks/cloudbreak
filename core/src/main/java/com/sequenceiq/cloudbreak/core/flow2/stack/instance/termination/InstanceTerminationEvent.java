package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum InstanceTerminationEvent implements FlowEvent {
    TERMINATION_EVENT("REMOVE_INSTANCE_TRIGGER_EVENT"),
    TERMINATION_FINISHED_EVENT(CloudPlatformResult.selector(RemoveInstanceResult.class)),
    TERMINATION_FAILED_EVENT(CloudPlatformResult.failureSelector(RemoveInstanceResult.class)),
    TERMINATION_FINALIZED_EVENT("TERMINATEINSTANCEFINALIZED"),
    TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONINSTANCEFAILHANDLED");

    private final String event;

    InstanceTerminationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
