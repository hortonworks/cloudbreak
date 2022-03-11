package com.sequenceiq.datalake.flow.detach;

import com.sequenceiq.datalake.flow.detach.event.SdxDetachFailedEvent;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SdxDetachEvent implements FlowEvent {
    SDX_DETACH_EVENT(),
    SDX_DETACH_FAILED_EVENT(SdxDetachFailedEvent.class),
    SDX_DETACH_CLUSTER_SUCCESS_EVENT(),
    SDX_DETACH_STACK_SUCCESS_EVENT(),
    SDX_DETACH_STACK_SUCCESS_WITH_EXTERNAL_DB_EVENT(),
    SDX_DETACH_STACK_FAILED_EVENT(),
    SDX_DETACH_EXTERNAL_DB_SUCCESS_EVENT(),
    SDX_DETACH_EXTERNAL_DB_FAILED_EVENT(),
    SDX_ATTACH_NEW_CLUSTER_SUCCESS_EVENT(),
    SDX_ATTACH_NEW_CLUSTER_FAILED_EVENT(),
    SDX_DETACH_FAILED_HANDLED_EVENT();

    private final String event;

    SdxDetachEvent() {
        this.event = name();
    }

    SdxDetachEvent(Class eventClass) {
        this.event = EventSelectorUtil.selector(eventClass);
    }

    @Override
    public String event() {
        return event;
    }

}
