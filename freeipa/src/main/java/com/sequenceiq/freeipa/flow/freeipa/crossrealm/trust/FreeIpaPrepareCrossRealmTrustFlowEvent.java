package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.ConfigureDnsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustSuccess;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareIpaServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareIpaServerSuccess;

public enum FreeIpaPrepareCrossRealmTrustFlowEvent implements FlowEvent {
    PREPARE_CROSS_REALM_TRUST_EVENT(EventSelectorUtil.selector(PrepareCrossRealmTrustEvent.class)),
    VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(CrossRealmTrustValidationSuccess.class)),
    VALIDATION_FAILED_EVENT(EventSelectorUtil.selector(CrossRealmTrustValidationFailed.class)),
    PREPARE_IPA_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(PrepareIpaServerSuccess.class)),
    PREPARE_IPA_SERVER_FAILED_EVENT(EventSelectorUtil.selector(PrepareIpaServerFailed.class)),
    CONFIGURE_DNS_FINISHED_EVENT(EventSelectorUtil.selector(ConfigureDnsSuccess.class)),
    CONFIGURE_DNS_FAILED_EVENT(EventSelectorUtil.selector(ConfigureDnsFailed.class)),
    PREPARE_CROSS_REALM_TRUST_FINISHED_EVENT(EventSelectorUtil.selector(PrepareCrossRealmTrustSuccess.class)),
    PREPARE_CROSS_REALM_TRUST_FAILURE_EVENT(EventSelectorUtil.selector(PrepareCrossRealmTrustFailureEvent.class)),
    PREPARE_CROSS_REALM_TRUST_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaPrepareCrossRealmTrustFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaPrepareCrossRealmTrustFlowEvent() {
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
