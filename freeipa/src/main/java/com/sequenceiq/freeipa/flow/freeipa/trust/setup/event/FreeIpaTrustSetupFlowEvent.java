package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum FreeIpaTrustSetupFlowEvent implements FlowEvent {
    TRUST_SETUP_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupEvent.class)),
    TRUST_SETUP_VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupValidationSuccess.class)),
    TRUST_SETUP_VALIDATION_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupValidationFailed.class)),
    TRUST_SETUP_PREPARE_IPA_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupPrepareServerSuccess.class)),
    TRUST_SETUP_PREPARE_IPA_SERVER_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupPrepareServerFailed.class)),
    TRUST_SETUP_CONFIGURE_DNS_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupConfigureDnsSuccess.class)),
    TRUST_SETUP_CONFIGURE_DNS_FAILED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupConfigureDnsFailed.class)),
    TRUST_SETUP_FINISHED_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupSuccess.class)),
    TRUST_SETUP_FAILURE_EVENT(EventSelectorUtil.selector(FreeIpaTrustSetupFailureEvent.class)),
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
