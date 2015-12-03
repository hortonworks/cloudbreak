package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum StackCreationEvent implements FlowEvent {
    START_CREATION_EVENT(FlowPhases.PROVISIONING_SETUP.name()),
    SETUP_FINISHED_EVENT(SetupResult.selector(SetupResult.class)),
    SETUP_FAILED_EVENT(SetupResult.failureSelector(SetupResult.class)),
    IMAGE_PREPARATION_FINISHED_EVENT(PrepareImageResult.selector(PrepareImageResult.class)),
    IMAGE_PREPARATION_FAILED_EVENT(PrepareImageResult.failureSelector(PrepareImageResult.class)),
    IMAGE_COPY_CHECK_EVENT("IMAGECOPYCHECK"),
    IMAGE_COPY_FINISHED_EVENT("IMAGECOPYFINISHED"),
    IMAGE_COPY_FAILED_EVENT("IMAGECOPYFAILED"),
    LAUNCH_STACK_FINISHED_EVENT(LaunchStackResult.selector(LaunchStackResult.class)),
    LAUNCH_STACK_FAILED_EVENT(LaunchStackResult.failureSelector(LaunchStackResult.class)),
    STACK_CREATION_FINISHED_EVENT("LAUNCHSTACKFINISHED"),
    STACK_CREATION_FAILED_EVENT("FAILHANDLED");

    private String stringRepresentation;

    StackCreationEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }

    public static StackCreationEvent fromString(String stringRepresentiation) {
        for (StackCreationEvent event : StackCreationEvent.values()) {
            if (stringRepresentiation.equalsIgnoreCase(event.stringRepresentation)) {
                return event;
            }
        }
        return null;
    }
}
