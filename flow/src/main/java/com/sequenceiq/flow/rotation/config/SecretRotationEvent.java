package com.sequenceiq.flow.rotation.config;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.flow.rotation.event.SecretRotationTriggerEvent;

public enum SecretRotationEvent implements FlowEvent {

    SECRET_ROTATION_TRIGGER_EVENT(EventSelectorUtil.selector(SecretRotationTriggerEvent.class)),
    EXECUTE_ROTATION_FINISHED_EVENT(EventSelectorUtil.selector(ExecuteRotationFinishedEvent.class)),
    EXECUTE_ROTATION_FAILED_EVENT(EventSelectorUtil.selector(ExecuteRotationFailedEvent.class)),
    ROTATION_FAILED_EVENT(EventSelectorUtil.selector(RotationFailedEvent.class)),
    ROTATION_FAILURE_HANDLED_EVENT("ROTATION_FAILURE_HANDLED_EVENT"),
    ROTATION_FINISHED_EVENT("ROTATION_FINISHED_EVENT");

    private final String selector;

    SecretRotationEvent(String selector) {
        this.selector = selector;
    }

    @Override
    public String event() {
        return selector;
    }
}
