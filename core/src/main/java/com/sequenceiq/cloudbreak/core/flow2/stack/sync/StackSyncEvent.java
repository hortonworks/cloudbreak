package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;

public enum StackSyncEvent implements FlowEvent {
    SYNC_EVENT(FlowTriggers.STACK_SYNC_TRIGGER_EVENT),
    SYNC_FINISHED_EVENT(GetInstancesStateResult.selector(GetInstancesStateResult.class)),
    SYNC_FAILURE_EVENT(GetInstancesStateResult.failureSelector(GetInstancesStateResult.class)),
    SYNC_FINALIZED_EVENT("SYNCSTACKFINALIZED"),
    SYNC_FAIL_HANDLED_EVENT("SYNCFAILHANDLED");

    private String stringRepresentation;

    StackSyncEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
