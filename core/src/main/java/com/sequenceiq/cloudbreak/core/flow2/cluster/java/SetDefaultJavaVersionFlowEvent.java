package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler.SetDefaultJavaVersionResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SetDefaultJavaVersionFlowEvent implements FlowEvent {

    SET_DEFAULT_JAVA_VERSION_EVENT,
    SET_DEFAULT_JAVA_VERSION_FINISHED_EVENT(EventSelectorUtil.selector(SetDefaultJavaVersionResult.class)),
    SET_DEFAULT_JAVA_VERSION_FINALIZED_EVENT,
    SET_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT,
    SET_DEFAULT_JAVA_VERSION_FAILED_EVENT(EventSelectorUtil.selector(SetDefaultJavaVersionFailedEvent.class));

    private final String event;

    SetDefaultJavaVersionFlowEvent() {
        event = name();
    }

    SetDefaultJavaVersionFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
