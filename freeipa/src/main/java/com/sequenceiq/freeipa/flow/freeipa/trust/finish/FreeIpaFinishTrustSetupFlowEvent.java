package com.sequenceiq.freeipa.flow.freeipa.trust.finish;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupSuccess;

public enum FreeIpaFinishTrustSetupFlowEvent implements FlowEvent {
    FINISH_TRUST_SETUP_EVENT(EventSelectorUtil.selector(FinishTrustSetupEvent.class)),
    ADD_TRUST_FINISHED_EVENT(EventSelectorUtil.selector(FinishTrustSetupAddTrustSuccess.class)),
    ADD_TRUST_FAILED_EVENT(EventSelectorUtil.selector(FinishTrustSetupAddTrustFailed.class)),
    FINISH_TRUST_SETUP_FINISHED_EVENT(EventSelectorUtil.selector(FinishTrustSetupSuccess.class)),
    FINISH_TRUST_SETUP_FAILURE_EVENT(EventSelectorUtil.selector(FinishTrustSetupFailureEvent.class)),
    FINISH_TRUST_SETUP_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaFinishTrustSetupFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaFinishTrustSetupFlowEvent() {
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
