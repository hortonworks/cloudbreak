package com.sequenceiq.cloudbreak.rotation.flow.rotation.config;

import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.PreValidateRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.rotation.event.SecretRotationTriggerEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SecretRotationEvent implements FlowEvent {

    SECRET_ROTATION_TRIGGER_EVENT(EventSelectorUtil.selector(SecretRotationTriggerEvent.class)),
    PRE_VALIDATE_ROTATION_FINISHED_EVENT(EventSelectorUtil.selector(PreValidateRotationFinishedEvent.class)),
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
