package com.sequenceiq.environment.environment.flow.hybrid.setup.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustSetupStateSelectors implements FlowEvent {

    TRUST_SETUP_VALIDATION_EVENT,
    TRUST_SETUP_EVENT,
    FINISH_TRUST_SETUP_EVENT,
    FINALIZE_TRUST_SETUP_EVENT,
    HANDLED_FAILED_TRUST_SETUP_EVENT,
    FAILED_TRUST_SETUP_EVENT;

    @Override
    public String event() {
        return name();
    }
}
