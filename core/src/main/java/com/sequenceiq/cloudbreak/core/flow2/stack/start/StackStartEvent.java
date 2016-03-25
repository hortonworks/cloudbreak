package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackStartEvent implements FlowEvent {
    START_EVENT(FlowPhases.STACK_START.name()),
    START_FINISHED_EVENT(StartInstancesResult.selector(StartInstancesResult.class)),
    START_FAILURE_EVENT(StartInstancesResult.failureSelector(StartInstancesResult.class)),
    START_FINALIZED_EVENT("STARTSTACKFINALIZED"),
    START_FAIL_HANDLED_EVENT("STARTFAILHANDLED");

    private String stringRepresentation;

    StackStartEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }

}
