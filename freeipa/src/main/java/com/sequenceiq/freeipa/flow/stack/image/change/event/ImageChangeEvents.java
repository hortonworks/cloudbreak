package com.sequenceiq.freeipa.flow.stack.image.change.event;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum ImageChangeEvents implements FlowEvent {
    IMAGE_CHANGE_EVENT,
    IMAGE_CHANGED_IN_DB_EVENT,
    IMAGE_CHANGE_NOT_REQUIRED_EVENT,
    IMAGE_PREPARATION_FINISHED_EVENT(CloudPlatformResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(CloudPlatformResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT,
    SET_IMAGE_ON_PROVIDER_FINISHED_EVENT(CloudPlatformResult.selector(UpdateImageResult.class)),
    SET_IMAGE_ON_PROVIDER_FAILED_EVENT(CloudPlatformResult.failureSelector(UpdateImageResult.class)),
    IMAGE_COPY_FINISHED_EVENT,
    IMAGE_CHANGE_FAILED_EVENT,
    IMAGE_CHANGE_FAILURE_HANDLED_EVENT,
    IMAGE_CHANGE_FINISHED_EVENT;

    private final String event;

    ImageChangeEvents(String event) {
        this.event = event;
    }

    ImageChangeEvents() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
