package com.sequenceiq.datalake.flow.detach;

import com.sequenceiq.datalake.flow.detach.event.SdxDetachRecoveryFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxDetachRecoveryEvent implements FlowEvent {
    SDX_DETACH_RECOVERY_EVENT(),
    SDX_DETACH_RECOVERY_SUCCESS_EVENT(),
    SDX_DETACH_RECOVERY_FAILED_EVENT(SdxDetachRecoveryFailedEvent.class),
    SDX_DETACH_RECOVERY_FAILURE_HANDLED_EVENT();

    private final String event;

    SdxDetachRecoveryEvent() {
        event = name();
    }

    SdxDetachRecoveryEvent(Class eventClass) {
        event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }
}
