package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;

public enum StackStopEvent implements FlowEvent {
    STOP_EVENT(FlowTriggers.STACK_STOP_TRIGGER_EVENT),
    STOP_FINISHED_EVENT(StopInstancesResult.selector(StopInstancesResult.class)),
    STOP_FAILURE_EVENT(StopInstancesResult.failureSelector(StopInstancesResult.class)),
    STOP_FINALIZED_EVENT("STOPSTACKFINALIZED"),
    STOP_FAIL_HANDLED_EVENT("STOPFAILHANDLED");

    private String stringRepresentation;

    StackStopEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }

}
