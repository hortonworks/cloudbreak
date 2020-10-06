package com.sequenceiq.datalake.flow.cert.rotation.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxCertRotationEvent implements FlowEvent {
    ROTATE_CERT_EVENT(SdxStartCertRotationEvent.class),
    CERT_ROTATION_STARTED_EVENT,
    CERT_ROTATION_FINISHED_EVENT,
    CERT_ROTATION_FINALIZED_EVENT,
    CERT_ROTATION_FAILED_EVENT(SdxCertRotationFailedEvent.class),
    CERT_ROTATION_FAILURE_HANDLED_EVENT;

    private final String event;

    SdxCertRotationEvent() {
        event = name();
    }

    SdxCertRotationEvent(String event) {
        this.event = event;
    }

    SdxCertRotationEvent(Class<?> clazz) {
        event = EventSelectorUtil.selector(clazz);
    }

    @Override
    public String event() {
        return event;
    }
}
