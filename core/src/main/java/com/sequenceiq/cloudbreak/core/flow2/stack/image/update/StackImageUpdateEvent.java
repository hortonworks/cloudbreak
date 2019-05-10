package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum StackImageUpdateEvent implements FlowEvent {
    STACK_IMAGE_UPDATE_EVENT("STACK_IMAGE_UPDATE_EVENT"),
    STACK_IMAGE_UPDATE_FAILED_EVENT("STACK_IMAGE_UPDATE_FAILED_EVENT"),
    STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT("STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT"),
    CHECK_IMAGE_VERESIONS_FINISHED_EVENT("CHECK_IMAGE_VERESIONS_FINISHED_EVENT"),
    CHECK_PACKAGE_VERSIONS_FINISHED_EVENT("CHECK_PACKAGE_VERSIONS_FINISHED_EVENT"),
    IMAGE_PREPARATION_FINISHED_EVENT(CloudPlatformResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(CloudPlatformResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT("IMAGE_COPY_CHECK_EVENT"),
    IMAGE_COPY_FINISHED_EVENT("IMAGE_COPY_FINISHED_EVENT"),
    UPDATE_IMAGE_FINESHED_EVENT("UPDATE_IMAGE_FINESHED_EVENT"),
    SET_IMAGE_FINISHED_EVENT(CloudPlatformResult.selector(UpdateImageResult.class)),
    SET_IMAGE_FAILED_EVENT(CloudPlatformResult.failureSelector(UpdateImageResult.class)),
    STACK_IMAGE_UPDATE_FINISHED_EVENT("STACK_IMAGE_UPDATE_FINISHED_EVENT");

    private final String event;

    StackImageUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
