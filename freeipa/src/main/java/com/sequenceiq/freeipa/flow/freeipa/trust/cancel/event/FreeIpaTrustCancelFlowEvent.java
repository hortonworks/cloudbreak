package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaTrustCancelFlowEvent implements FlowEvent {
    TRUST_CANCEL_EVENT(EventSelectorUtil.selector(FreeIpaTrustCancelEvent.class)),
    TRUST_CANCEL_CONFIGURATION_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustCancelConfigurationSuccess.class)),
    TRUST_CANCEL_CONFIGURATION_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaTrustCancelConfigurationFailed.class)),
    TRUST_CANCEL_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustCancelSuccess.class)),
    TRUST_CANCEL_FAILURE_EVENT(EventSelectorUtil.selector(FreeIpaTrustCancelFailureEvent.class)),
    TRUST_CANCEL_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaTrustCancelFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaTrustCancelFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }

    @Override
    public String selector() {
        return event;
    }
}
