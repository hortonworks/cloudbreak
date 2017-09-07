package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackSyncEvent implements FlowEvent {
    STACK_SYNC_EVENT("STACK_SYNC_TRIGGER_EVENT"),
    SYNC_FINISHED_EVENT(GetInstancesStateResult.selector(GetInstancesStateResult.class)),
    SYNC_FAILURE_EVENT(GetInstancesStateResult.failureSelector(GetInstancesStateResult.class)),
    SYNC_FINALIZED_EVENT("SYNCSTACKFINALIZED"),
    SYNC_FAIL_HANDLED_EVENT("SYNCFAILHANDLED");

    private final String event;

    StackSyncEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
