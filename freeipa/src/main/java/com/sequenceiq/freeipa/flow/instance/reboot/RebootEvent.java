package com.sequenceiq.freeipa.flow.instance.reboot;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;
import com.sequenceiq.freeipa.flow.stack.HealthCheckSuccess;

public enum RebootEvent implements FlowEvent {
    REBOOT_EVENT("REBOOT_TRIGGER_EVENT"),
    REBOOT_FINISHED_EVENT(CloudPlatformResult.selector(RebootInstancesResult.class)),
    REBOOT_FAILURE_EVENT(CloudPlatformResult.failureSelector(RebootInstancesResult.class)),
    REBOOT_WAIT_UNTIL_AVAILABLE_FINISHED_EVENT(EventSelectorUtil.selector(HealthCheckSuccess.class)),
    REBOOT_WAIT_UNTIL_AVAILABLE_FAILURE_EVENT(EventSelectorUtil.selector(HealthCheckFailed.class)),
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
