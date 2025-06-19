package com.sequenceiq.freeipa.flow.freeipa.trust.setup;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.ConfigureDnsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationSuccess;

public enum FreeIpaTrustSetupFlowEvent implements FlowEvent {
    TRUST_SETUP_EVENT(EventSelectorUtil.selector(TrustSetupEvent.class)),
    VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(TrustSetupValidationSuccess.class)),
    VALIDATION_FAILED_EVENT(EventSelectorUtil.selector(TrustSetupValidationFailed.class)),
    PREPARE_IPA_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(PrepareIpaServerSuccess.class)),
    PREPARE_IPA_SERVER_FAILED_EVENT(EventSelectorUtil.selector(PrepareIpaServerFailed.class)),
    CONFIGURE_DNS_FINISHED_EVENT(EventSelectorUtil.selector(ConfigureDnsSuccess.class)),
    CONFIGURE_DNS_FAILED_EVENT(EventSelectorUtil.selector(ConfigureDnsFailed.class)),
    TRUST_SETUP_FINISHED_EVENT(EventSelectorUtil.selector(TrustSetupSuccess.class)),
    TRUST_SETUP_FAILURE_EVENT(EventSelectorUtil.selector(TrustSetupFailureEvent.class)),
    TRUST_SETUP_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaTrustSetupFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaTrustSetupFlowEvent() {
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
