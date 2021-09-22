package com.sequenceiq.freeipa.flow.stack.update;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateOnProviderResult;
import com.sequenceiq.freeipa.flow.stack.update.event.UserDataUpdateSuccess;

public enum UpdateUserDataEvents implements FlowEvent {
    UPDATE_USERDATA_TRIGGER_EVENT,
    UPDATE_USERDATA_IN_DB_FINISHED_EVENT(UserDataUpdateOnProviderResult.selector(UserDataUpdateSuccess.class)),
    UPDATE_USERDATA_ON_PROVIDER_FINISHED_EVENT(UserDataUpdateOnProviderResult.selector(UserDataUpdateOnProviderResult.class)),
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
