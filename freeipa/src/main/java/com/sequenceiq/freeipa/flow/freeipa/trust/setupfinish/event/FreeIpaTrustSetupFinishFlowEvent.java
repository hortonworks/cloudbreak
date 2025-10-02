package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaTrustSetupFinishFlowEvent implements FlowEvent {
    TRUST_SETUP_FINISH_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFinishEvent.class)),
    TRUST_SETUP_FINISH_ADD_TRUST_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFinishAddTrustSuccess.class)),
    TRUST_SETUP_FINISH_ADD_TRUST_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFinishAddTrustFailed.class)),
    TRUST_SETUP_FINISH_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFinishSuccess.class)),
    TRUST_SETUP_FINISH_FAILURE_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFinishFailureEvent.class)),
    TRUST_SETUP_FINISH_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaTrustSetupFinishFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaTrustSetupFinishFlowEvent() {
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
