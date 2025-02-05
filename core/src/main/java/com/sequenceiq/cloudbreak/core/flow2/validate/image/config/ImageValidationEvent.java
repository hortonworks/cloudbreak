package com.sequenceiq.cloudbreak.core.flow2.validate.image.config;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateImageResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum ImageValidationEvent implements FlowEvent {
    IMAGE_VALIDATION_EVENT("IMAGE_VALIDATION_EVENT"),
    IMAGE_VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(ValidateImageResult.class)),
    IMAGE_VALIDATION_FAILURE_HANDLED_EVENT("IMAGE_VALIDATION_FAILURE_HANDLED_EVENT"),
    IMAGE_VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(ValidateImageResult.class));

    private final String event;

    ImageValidationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
