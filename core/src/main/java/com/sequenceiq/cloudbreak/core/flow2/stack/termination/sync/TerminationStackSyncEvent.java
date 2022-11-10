package com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum TerminationStackSyncEvent implements FlowEvent {
    TERMINATION_STACK_SYNC_EVENT("TERMINATION_STACK_SYNC_TRIGGER_EVENT"),
    TERMINATION_SYNC_FINISHED_EVENT(CloudPlatformResult.selector(GetInstancesStateResult.class)),
    TERMINATION_SYNC_FAILURE_EVENT(CloudPlatformResult.failureSelector(GetInstancesStateResult.class)),
    TERMINATION_SYNC_FINALIZED_EVENT("TERMINATIONSYNCSTACKFINALIZED"),
    TERMINATION_SYNC_FAIL_HANDLED_EVENT("TERMINATIONSYNCFAILHANDLED");

    private final String event;

    TerminationStackSyncEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
