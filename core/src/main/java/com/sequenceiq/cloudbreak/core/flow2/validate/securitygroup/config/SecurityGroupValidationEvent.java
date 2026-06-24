package com.sequenceiq.cloudbreak.core.flow2.validate.securitygroup.config;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum SecurityGroupValidationEvent implements FlowEvent {
    SECURITY_GROUP_VALIDATION_EVENT,
    SECURITY_GROUP_VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(SecurityGroupValidationResult.class)),
    SECURITY_GROUP_VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(SecurityGroupValidationResult.class)),
    SECURITY_GROUP_VALIDATION_FINALIZED_EVENT,
    SECURITY_GROUP_VALIDATION_FAIL_EVENT,
    SECURITY_GROUP_VALIDATION_FAILURE_HANDLED_EVENT;

    private final String event;

    SecurityGroupValidationEvent(String event) {
        this.event = event;
    }

    SecurityGroupValidationEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
