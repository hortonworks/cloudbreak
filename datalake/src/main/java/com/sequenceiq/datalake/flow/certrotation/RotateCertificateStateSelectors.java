package com.sequenceiq.datalake.flow.certrotation;

import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateFailedEvent;
import com.sequenceiq.datalake.flow.certrotation.event.RotateCertificateSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum RotateCertificateStateSelectors implements FlowEvent {

    ROTATE_CERTIFICATE_STACK_EVENT,
    ROTATE_CERTIFICATE_SUCCESS_EVENT(RotateCertificateSuccessEvent.class),
    ROTATE_CERTIFICATE_FAILED_EVENT(RotateCertificateFailedEvent.class),
    ROTATE_CERTIFICATE_FAILED_HANDLED_EVENT,
    ROTATE_CERTIFICATE_FINALIZED_EVENT;

    private final String event;

    RotateCertificateStateSelectors() {
        event = name();
    }

    RotateCertificateStateSelectors(Class<?> eventClass) {
        event = eventClass.getSimpleName();
    }

    @Override
    public String event() {
        return event;
    }

}
