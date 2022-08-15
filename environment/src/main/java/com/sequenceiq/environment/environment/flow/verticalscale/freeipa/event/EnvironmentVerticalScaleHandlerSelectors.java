package com.sequenceiq.environment.environment.flow.verticalscale.freeipa.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum EnvironmentVerticalScaleHandlerSelectors implements FlowEvent {

    VERTICAL_SCALING_FREEIPA_VALIDATION_HANDLER,
    VERTICAL_SCALING_FREEIPA_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
