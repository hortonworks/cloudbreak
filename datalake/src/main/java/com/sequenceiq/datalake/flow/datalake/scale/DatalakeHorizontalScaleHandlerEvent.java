package com.sequenceiq.datalake.flow.datalake.scale;

import com.sequenceiq.flow.core.FlowEvent;

public enum DatalakeHorizontalScaleHandlerEvent implements FlowEvent {

    DATALAKE_HORIZONTAL_SCALE_IN_PROGRESS_HANDLER,
    DATALAKE_HORIZONTAL_SCALE_CM_ROLLING_RESTART_IN_PROGRESS_HANDLER;

    @Override
    public String event() {
        return name();
    }
}
