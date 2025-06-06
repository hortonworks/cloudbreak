package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustSuccess;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustSuccess;

public enum FreeIpaFinishCrossRealmTrustFlowEvent implements FlowEvent {
    FINISH_CROSS_REALM_TRUST_EVENT(EventSelectorUtil.selector(FinishCrossRealmTrustEvent.class)),
    ADD_TRUST_FINISHED_EVENT(EventSelectorUtil.selector(FinishCrossRealmTrustAddTrustSuccess.class)),
    ADD_TRUST_FAILED_EVENT(EventSelectorUtil.selector(FinishCrossRealmTrustAddTrustFailed.class)),
    FINISH_CROSS_REALM_TRUST_FINISHED_EVENT(EventSelectorUtil.selector(FinishCrossRealmTrustSuccess.class)),
    FINISH_CROSS_REALM_TRUST_FAILURE_EVENT(EventSelectorUtil.selector(FinishCrossRealmTrustFailureEvent.class)),
    FINISH_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaFinishCrossRealmTrustFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaFinishCrossRealmTrustFlowEvent() {
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
