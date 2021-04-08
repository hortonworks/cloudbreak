package com.sequenceiq.freeipa.flow.freeipa.binduser.create.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum CreateBindUserFlowEvent implements FlowEvent {
    CREATE_BIND_USER_EVENT,
    CREATE_KERBEROS_BIND_USER_FINISHED_EVENT,
    CREATE_LDAP_BIND_USER_FINISHED_EVENT,

    CREATE_BIND_USER_FINISHED_EVENT,
    CREATE_BIND_USER_FAILED_EVENT(EventSelectorUtil.selector(CreateBindUserFailureEvent.class)),
    CREATE_BIND_USER_FAILURE_HANDLED_EVENT;

    private final String event;

    CreateBindUserFlowEvent(String event) {
        this.event = event;
    }

    CreateBindUserFlowEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
