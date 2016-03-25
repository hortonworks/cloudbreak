package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackStopEvent implements FlowEvent {
    STOP_EVENT(FlowPhases.STACK_STOP.name()),
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
