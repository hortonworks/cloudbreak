package com.sequenceiq.environment.environment.flow.hybrid.cancel.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustCancelHandlerSelectors implements FlowEvent {

    TRUST_CANCEL_VALIDATION_HANDLER,
    TRUST_CANCEL_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
