package com.sequenceiq.environment.environment.flow.hybrid.setup.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustSetupHandlerSelectors implements FlowEvent {

    TRUST_SETUP_VALIDATION_HANDLER,
    TRUST_SETUP_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
