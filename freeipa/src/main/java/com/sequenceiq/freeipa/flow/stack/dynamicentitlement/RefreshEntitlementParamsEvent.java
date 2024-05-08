package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum RefreshEntitlementParamsEvent implements FlowEvent {

    REFRESH_ENTITLEMENT_PARAMS_TRIGGER_EVENT(EventSelectorUtil.selector(RefreshEntitlementParamsTriggerEvent.class)),

    REFRESH_ENTITLEMENT_FINALIZED_EVENT,
    REFRESH_ENTITLEMENT_FAILURE_EVENT,
    REFRESH_ENTITLEMENT_FAIL_HANDLED_EVENT;

    private final String event;

    RefreshEntitlementParamsEvent(String event) {
        this.event = event;
    }

    RefreshEntitlementParamsEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
