package com.sequenceiq.freeipa.flow.instance.reboot;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum RebootEvent implements FlowEvent {
    REBOOT_EVENT("REBOOT_TRIGGER_EVENT"),
    REBOOT_FINISHED_EVENT(CloudPlatformResult.selector(RebootInstancesResult.class)),
    REBOOT_FAILURE_EVENT(CloudPlatformResult.failureSelector(RebootInstancesResult.class)),
    REBOOT_FINALIZED_EVENT("REBOOTFINALIZED"),
    REBOOT_FAIL_HANDLED_EVENT("REBOOTFAILHANDLED");

    private final String event;

    RebootEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
