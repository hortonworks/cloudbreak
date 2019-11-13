package com.sequenceiq.datalake.flow.start;

import com.sequenceiq.flow.core.FlowEvent;

public enum SdxStartEvent implements FlowEvent {

    SDX_START_EVENT("SDX_START_EVENT"),
    SDX_SYNC_EVENT("SDX_SYNC_EVENT"),
    SDX_SYNC_FINISHED_EVENT("SdxSyncSuccessEvent"),
    SDX_START_IN_PROGRESS_EVENT("SDX_START_IN_PROGRESS_EVENT"),
    SDX_START_SUCCESS_EVENT("SdxStartSuccessEvent"),
    SDX_START_FAILED_EVENT("SdxStartFailedEvent"),
    SDX_START_FAILED_HANDLED_EVENT("SDX_START_FAILED_HANDLED_EVENT"),
    SDX_START_FINALIZED_EVENT("SDX_START_FINALIZED_EVENT");

    private final String event;

    SdxStartEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }

}
