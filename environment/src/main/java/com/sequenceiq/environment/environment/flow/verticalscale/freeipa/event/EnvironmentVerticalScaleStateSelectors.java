package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentVerticalScaleStateSelectors implements FlowEvent {

    VERTICAL_SCALING_FREEIPA_VALIDATION_EVENT,
    VERTICAL_SCALING_FREEIPA_EVENT,
    FINISH_VERTICAL_SCALING_FREEIPA_EVENT,
    FINALIZE_VERTICAL_SCALING_FREEIPA_EVENT,
    HANDLED_FAILED_VERTICAL_SCALING_FREEIPA_EVENT,
    FAILED_VERTICAL_SCALING_FREEIPA_EVENT;

    @Override
    public String event() {
        return name();
    }
}
