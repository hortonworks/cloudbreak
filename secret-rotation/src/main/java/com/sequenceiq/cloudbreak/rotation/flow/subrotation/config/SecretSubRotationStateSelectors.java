package com.sequenceiq.cloudbreak.rotation.flow.subrotation.config;

import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SecretSubRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SubRotationFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SecretSubRotationStateSelectors implements FlowEvent {

    SECRET_SUB_ROTATION_TRIGGER_EVENT(EventSelectorUtil.selector(SecretSubRotationTriggerEvent.class)),
    SUB_ROTATION_FAILED_EVENT(EventSelectorUtil.selector(SubRotationFailedEvent.class)),
    SUB_ROTATION_FINISHED_EVENT(EventSelectorUtil.selector(ExecuteSubRotationFinishedEvent.class)),
    SUB_ROTATION_FAILURE_HANDLED_EVENT("SUB_ROTATION_FAILURE_HANDLED_EVENT");

    private final String selector;

    SecretSubRotationStateSelectors(String selector) {
        this.selector = selector;
    }

    @Override
    public String event() {
        return selector;
    }
}
