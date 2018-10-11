package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

public enum MaintenanceModeValidationEvent implements FlowEvent {
    START_VALIDATION_FLOW_EVENT("START_VALIDATION_FLOW_EVENT"),
    FETCH_STACK_REPO_INFO_FINISHED_EVENT("FETCH_STACK_REPO_INFO_FINISHED_EVENT"),
    VALIDATE_STACK_REPO_INFO_FINISHED_EVENT("VALIDATE_STACK_REPO_INFO_FINISHED_EVENT"),
    VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT("VALIDATE_AMBARI_REPO_INFO_FINISHED_EVENT"),
    VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT("VALIDATE_IMAGE_COMPATIBILITY_FINISHED_EVENT"),

    VALIDATION_FLOW_FINISHED_EVENT("VALIDATION_FLOW_FINISHED_EVENT"),
    VALIDATION_FLOW_FAILED_EVENT("VALIDATION_FLOW_FAILED_EVENT"),
    VALIDATION_FAIL_HANDLED_EVENT("VALIDATION_FAIL_HANDLED_EVENT");

    private final String event;

    MaintenanceModeValidationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
