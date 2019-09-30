package com.sequenceiq.freeipa.flow.stack.stop;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum StackStopEvent implements FlowEvent {
    STACK_STOP_EVENT("STACK_STOP_TRIGGER_EVENT"),
    STOP_FINISHED_EVENT(CloudPlatformResult.selector(StopInstancesResult.class)),
    STOP_FAILURE_EVENT(CloudPlatformResult.failureSelector(StopInstancesResult.class)),
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
