package com.sequenceiq.datalake.flow.verticalscale.event;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeVerticalScaleStateSelectors implements FlowEvent {

    VERTICAL_SCALING_DATALAKE_VALIDATION_EVENT,
    VERTICAL_SCALING_DATALAKE_EVENT,
    FINISH_VERTICAL_SCALING_DATALAKE_EVENT,
    FINALIZE_VERTICAL_SCALING_DATALAKE_EVENT,
    HANDLED_FAILED_VERTICAL_SCALING_DATALAKE_EVENT,
    FAILED_VERTICAL_SCALING_DATALAKE_EVENT;

    @Override
    public String event() {
        return name();
    }
}
