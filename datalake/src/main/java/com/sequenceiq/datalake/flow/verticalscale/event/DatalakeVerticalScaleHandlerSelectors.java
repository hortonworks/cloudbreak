package com.sequenceiq.datalake.flow.verticalscale.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeVerticalScaleHandlerSelectors implements FlowEvent {

    VERTICAL_SCALING_DATALAKE_VALIDATION_HANDLER,
    VERTICAL_SCALING_DATALAKE_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
