package com.sequenceiq.datalake.flow.java;

import com.sequenceiq.datalake.flow.java.handler.WaitSetDatalakeDefaultJavaVersionResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SetDatalakeDefaultJavaVersionFlowEvent implements FlowEvent {

    SET_DATALAKE_DEFAULT_JAVA_VERSION_EVENT,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISHED_EVENT(EventSelectorUtil.selector(WaitSetDatalakeDefaultJavaVersionResult.class)),
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FINALIZED_EVENT,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT,
    SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_EVENT(EventSelectorUtil.selector(SetDatalakeDefaultJavaVersionFailedEvent.class));

    private final String event;

    SetDatalakeDefaultJavaVersionFlowEvent() {
        event = name();
    }

    SetDatalakeDefaultJavaVersionFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
