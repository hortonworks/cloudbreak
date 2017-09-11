package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackStartEvent implements FlowEvent {
    STACK_START_EVENT("STACK_START_TRIGGER_EVENT"),
    START_FINISHED_EVENT(StartInstancesResult.selector(StartInstancesResult.class)),
    START_FAILURE_EVENT(StartInstancesResult.failureSelector(StartInstancesResult.class)),
    COLLECT_METADATA_FINISHED_EVENT(CollectMetadataResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CollectMetadataResult.failureSelector(CollectMetadataResult.class)),
    START_FINALIZED_EVENT("STARTSTACKFINALIZED"),
    START_FAIL_HANDLED_EVENT("STARTFAILHANDLED");

    private final String event;

    StackStartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
