package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;

enum InstanceTerminationEvent implements FlowEvent {
    TERMINATION_EVENT(FlowTriggers.REMOVE_INSTANCE_TRIGGER_EVENT),
    TERMINATION_FINISHED_EVENT(RemoveInstanceResult.selector(RemoveInstanceResult.class)),
    TERMINATION_FAILED_EVENT(RemoveInstanceResult.failureSelector(RemoveInstanceResult.class)),
    TERMINATION_FINALIZED_EVENT("TERMINATEINSTANCEFINALIZED"),
    TERMINATION_FAIL_HANDLED_EVENT("TERMINATIONINSTANCEFAILHANDLED");

    private String stringRepresentation;

    InstanceTerminationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
