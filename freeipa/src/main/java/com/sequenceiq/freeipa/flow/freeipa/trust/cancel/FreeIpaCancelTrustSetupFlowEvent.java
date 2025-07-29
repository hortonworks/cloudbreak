package com.sequenceiq.freeipa.flow.freeipa.trust.cancel;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupSuccess;

public enum FreeIpaCancelTrustSetupFlowEvent implements FlowEvent {
    CANCEL_TRUST_SETUP_EVENT(EventSelectorUtil.selector(CancelTrustSetupEvent.class)),
    CANCEL_TRUST_SETUP_CONFIGURATION_FINISHED_EVENT(EventSelectorUtil.selector(CancelTrustSetupConfigurationSuccess.class)),
    CANCEL_TRUST_SETUP_CONFIGURATION_FAILED_EVENT(EventSelectorUtil.selector(CancelTrustSetupConfigurationFailed.class)),
    CANCEL_TRUST_SETUP_FINISHED_EVENT(EventSelectorUtil.selector(CancelTrustSetupSuccess.class)),
    CANCEL_TRUST_SETUP_FAILURE_EVENT(EventSelectorUtil.selector(CancelTrustSetupFailureEvent.class)),
    CANCEL_TRUST_SETUP_FAILURE_HANDLED_EVENT;

    private final String event;

    FreeIpaCancelTrustSetupFlowEvent(String event) {
        this.event = event;
    }

    FreeIpaCancelTrustSetupFlowEvent() {
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
