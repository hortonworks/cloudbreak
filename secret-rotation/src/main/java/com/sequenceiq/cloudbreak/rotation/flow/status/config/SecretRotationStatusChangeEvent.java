package com.sequenceiq.cloudbreak.rotation.flow.status.config;

import com.sequenceiq.flow.core.FlowEvent;

public enum SecretRotationStatusChangeEvent implements FlowEvent {

    SECRET_ROTATION_STATUS_CHANGE_TRIGGER_EVENT("SECRET_ROTATION_STATUS_CHANGE_TRIGGER_EVENT"),
    SECRET_ROTATION_STATUS_CHANGE_FINISHED_EVENT("SECRET_ROTATION_STATUS_CHANGE_FINISHED_EVENT"),
    SECRET_ROTATION_STATUS_CHANGE_FAILED_EVENT("SECRET_ROTATION_STATUS_CHANGE_FAILED_EVENT"),
    SECRET_ROTATION_STATUS_CHANGE_FAIL_HANDLED_EVENT("SECRET_ROTATION_STATUS_CHANGE_FAIL_HANDLED_EVENT");

    private final String event;

    SecretRotationStatusChangeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
