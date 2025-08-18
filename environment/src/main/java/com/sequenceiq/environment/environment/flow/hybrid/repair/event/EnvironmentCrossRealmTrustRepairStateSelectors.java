package com.sequenceiq.environment.environment.flow.hybrid.repair.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentCrossRealmTrustRepairStateSelectors implements FlowEvent {

    TRUST_REPAIR_VALIDATION_EVENT,
    TRUST_REPAIR_EVENT,
    FINISH_TRUST_REPAIR_EVENT,
    FINALIZE_TRUST_REPAIR_EVENT,
    HANDLED_FAILED_TRUST_REPAIR_EVENT,
    FAILED_TRUST_REPAIR_EVENT;

    @Override
    public String event() {
        return name();
    }
}
