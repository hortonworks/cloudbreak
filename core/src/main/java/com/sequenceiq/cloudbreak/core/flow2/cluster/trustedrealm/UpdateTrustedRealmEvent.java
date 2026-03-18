package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpdateTrustedRealmEvent implements FlowEvent {
    UPDATE_TRUSTED_REALM_TRIGGER_EVENT,
    UPDATE_TRUSTED_REALM_SUCCESS_EVENT(EventSelectorUtil.selector(UpdateTrustedRealmResult.class)),
    UPDATE_TRUSTED_REALM_FAILED_EVENT(EventSelectorUtil.failureSelector(UpdateTrustedRealmResult.class)),
    UPDATE_TRUSTED_REALM_FINALIZED_EVENT,
    UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT;

    private final String event;

    UpdateTrustedRealmEvent() {
        this.event = name();
    }

    UpdateTrustedRealmEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}

