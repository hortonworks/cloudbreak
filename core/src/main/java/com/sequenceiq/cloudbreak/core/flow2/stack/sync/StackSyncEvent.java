package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

enum StackSyncEvent implements FlowEvent {

    SYNC_EVENT(FlowPhases.STACK_SYNC.name()),
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

    public static StackSyncEvent fromString(String stringRepresentiation) {
        for (StackSyncEvent event : StackSyncEvent.values()) {
            if (stringRepresentiation.equalsIgnoreCase(event.stringRepresentation)) {
                return event;
            }
        }
        return null;
    }
}
