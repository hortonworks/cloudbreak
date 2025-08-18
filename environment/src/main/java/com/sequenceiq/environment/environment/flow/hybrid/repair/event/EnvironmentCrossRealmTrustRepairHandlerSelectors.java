package com.sequenceiq.environment.environment.flow.hybrid.repair.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustRepairHandlerSelectors implements FlowEvent {

    TRUST_REPAIR_VALIDATION_HANDLER,
    TRUST_REPAIR_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
