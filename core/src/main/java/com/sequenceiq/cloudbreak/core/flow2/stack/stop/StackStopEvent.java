package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackStopEvent implements FlowEvent {
    STACK_STOP_EVENT("STACK_STOP_TRIGGER_EVENT"),
    STOP_FINISHED_EVENT(StopInstancesResult.selector(StopInstancesResult.class)),
    STOP_FAILURE_EVENT(StopInstancesResult.failureSelector(StopInstancesResult.class)),
    STOP_FINALIZED_EVENT("STOPSTACKFINALIZED"),
    STOP_FAIL_HANDLED_EVENT("STOPFAILHANDLED");

    private final String event;

    StackStopEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
