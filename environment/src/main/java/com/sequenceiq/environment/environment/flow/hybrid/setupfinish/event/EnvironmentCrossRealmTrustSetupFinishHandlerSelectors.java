package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustSetupFinishHandlerSelectors implements FlowEvent {

    SETUP_FINISH_TRUST_VALIDATION_HANDLER,
    SETUP_FINISH_TRUST_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
