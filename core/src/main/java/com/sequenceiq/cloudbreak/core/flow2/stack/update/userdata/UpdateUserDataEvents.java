package com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateOnProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpdateUserDataEvents implements FlowEvent {
    UPDATE_USERDATA_TRIGGER_EVENT,
    UPDATE_USERDATA_IN_DB_FINISHED_EVENT(EventSelectorUtil.selector(UserDataUpdateSuccess.class)),
    UPDATE_USERDATA_ON_PROVIDER_FINISHED_EVENT(EventSelectorUtil.selector(UserDataUpdateOnProviderResult.class)),
    UPDATE_USERDATA_FAILED_EVENT,
    UPDATE_USERDATA_FAILURE_HANDLED_EVENT,
    UPDATE_USERDATA_FINISHED_EVENT;

    private final String event;

    UpdateUserDataEvents(String event) {
        this.event = event;
    }

    UpdateUserDataEvents() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
