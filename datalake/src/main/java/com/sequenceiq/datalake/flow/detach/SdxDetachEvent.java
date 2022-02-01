package com.sequenceiq.datalake.flow.detach;

import com.sequenceiq.datalake.flow.detach.event.SdxDetachFailedEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachInprogressEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxDetachSuccessEvent;
import com.sequenceiq.flow.core.FlowEvent;

public enum SdxDetachEvent implements FlowEvent {
    SDX_DETACH_EVENT(),
    SDX_DETACH_IN_PROGRESS_EVENT(SdxDetachInprogressEvent.class),
    SDX_DETACH_SUCCESS_EVENT(SdxDetachSuccessEvent.class),
    SDX_DETACH_FAILED_EVENT(SdxDetachFailedEvent.class),
    SDX_DETACH_FAILED_HANDLED_EVENT(),
    SDX_DETACH_FINALIZED_EVENT();

    private final String event;

    SdxDetachEvent() {
        this.event = name();
    }

    SdxDetachEvent(Class eventClass) {
        this.event = eventClass.getSimpleName();
    }

    @Override
    public String event() {
        return event;
    }

}
