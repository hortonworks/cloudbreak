package com.sequenceiq.cloudbreak.core.flow2.validate.cloud.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum CloudConfigValidationEvent implements FlowEvent {
    VALIDATE_CLOUD_CONFIG_EVENT,
    VALIDATE_CLOUD_CONFIG_FAILED_EVENT,
    VALIDATE_CLOUD_CONFIG_FAILURE_HANDLED_EVENT,
    VALIDATE_CLOUD_CONFIG_FINISHED_EVENT;

    @Override
    public String event() {
        return name();
    }
}
