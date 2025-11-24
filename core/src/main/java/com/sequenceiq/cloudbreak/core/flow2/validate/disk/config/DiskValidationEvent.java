package com.sequenceiq.cloudbreak.core.flow2.validate.disk.config;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateDiskResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum DiskValidationEvent implements FlowEvent {
    DISK_VALIDATION_EVENT("DISK_VALIDATION_EVENT"),
    DISK_VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(ValidateDiskResult.class)),
    DISK_VALIDATION_FAILURE_HANDLED_EVENT("DISK_VALIDATION_FAILURE_HANDLED_EVENT"),
    DISK_VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(ValidateDiskResult.class));

    private final String event;

    DiskValidationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
