package com.sequenceiq.datalake.flow.trustedrealm;

import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmSuccessResponse;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpdateTrustedRealmTrackerEvent implements FlowEvent {

    UPDATE_TRUSTED_REALM_EVENT,
    UPDATE_TRUSTED_REALM_FAILED_EVENT(EventSelectorUtil.selector(UpdateTrustedRealmFailureResponse.class)),
    UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT,
    UPDATE_TRUSTED_REALM_SUCCESS_EVENT(EventSelectorUtil.selector(UpdateTrustedRealmSuccessResponse.class)),
    UPDATE_TRUSTED_REALM_FINISHED_EVENT;

    private final String event;

    UpdateTrustedRealmTrackerEvent() {
        this.event = name();
    }

    UpdateTrustedRealmTrackerEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
