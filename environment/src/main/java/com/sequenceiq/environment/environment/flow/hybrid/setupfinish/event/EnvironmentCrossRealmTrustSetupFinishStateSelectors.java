package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustSetupFinishStateSelectors implements FlowEvent {

    TRUST_SETUP_FINISH_VALIDATION_EVENT,
    BIDIRECTIONAL_TRUST_SETUP_EVENT,
    FINISH_BIDIRECTIONAL_TRUST_SETUP_EVENT,
    FINALIZE_TRUST_SETUP_FINISH_EVENT,
    HANDLED_FAILED_TRUST_SETUP_FINISH_EVENT,
    FAILED_TRUST_SETUP_FINISH_EVENT;

    @Override
    public String event() {
        return name();
    }
}
