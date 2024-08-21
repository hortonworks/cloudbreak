package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum RestartEvent implements FlowEvent {

    RESTART_TRIGGER_EVENT("RESTART_TRIGGER_EVENT"),
    RESTART_FINISHED_EVENT(CloudPlatformResult.selector(RestartInstancesResult.class)),
    RESTART_FAILURE_EVENT(CloudPlatformResult.failureSelector(RestartInstancesResult.class)),
    RESTART_FINALIZED_EVENT("RESTARTFINALIZED"),
    RESTART_FAIL_HANDLED_EVENT("RESTARTFAILHANDLED");

    private final String event;

    RestartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
