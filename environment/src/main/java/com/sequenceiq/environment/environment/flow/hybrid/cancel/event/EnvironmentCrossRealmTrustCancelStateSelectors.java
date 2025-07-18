package com.sequenceiq.environment.environment.flow.hybrid.cancel.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustCancelStateSelectors implements FlowEvent {

    TRUST_CANCEL_VALIDATION_EVENT,
    TRUST_CANCEL_EVENT,
    FINISH_TRUST_CANCEL_EVENT,
    FINALIZE_TRUST_CANCEL_EVENT,
    HANDLED_FAILED_TRUST_CANCEL_EVENT,
    FAILED_TRUST_CANCEL_EVENT;

    @Override
    public String event() {
        return name();
    }
}
