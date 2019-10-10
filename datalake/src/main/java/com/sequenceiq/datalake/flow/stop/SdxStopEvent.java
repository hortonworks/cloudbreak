package com.sequenceiq.datalake.flow.stop;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxStopEvent implements FlowEvent {

    SDX_STOP_EVENT("SDX_STOP_EVENT"),
    SDX_STOP_IN_PROGRESS_EVENT("SDX_STOP_IN_PROGRESS_EVENT"),
    SDX_STOP_SUCCESS_EVENT("SdxStopSuccessEvent"),
    SDX_STOP_FAILED_EVENT("SdxStopFailedEvent"),
    SDX_STOP_FAILED_HANDLED_EVENT("SDX_STOP_FAILED_HANDLED_EVENT"),
    SDX_STOP_FINALIZED_EVENT("SDX_STOP_FINALIZED_EVENT");

    private final String event;

    SdxStopEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
