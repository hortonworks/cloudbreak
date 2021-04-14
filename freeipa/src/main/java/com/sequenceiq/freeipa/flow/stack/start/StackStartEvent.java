package com.sequenceiq.freeipa.flow.stack.start;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;

public enum StackStartEvent implements FlowEvent {
    STACK_START_EVENT("STACK_START_TRIGGER_EVENT"),
    START_FINISHED_EVENT(CloudPlatformResult.selector(StartInstancesResult.class)),
    START_FAILURE_EVENT(CloudPlatformResult.failureSelector(StartInstancesResult.class)),
    COLLECT_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(CollectMetadataResult.class)),
    COLLECT_METADATA_FAILED_EVENT(CloudPlatformResult.failureSelector(CollectMetadataResult.class)),
    START_SAVE_METADATA_FINISHED_EVENT("START_SAVE_METADATA_FINISHED"),
    START_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT(CloudPlatformResult.selector(HealthCheckSuccess.class)),
    START_WAIT_UNTIL_AVAILABLE_FAILED_EVENT(CloudPlatformResult.selector(HealthCheckFailed.class)),
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
