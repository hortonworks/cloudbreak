package com.sequenceiq.datalake.flow.datahub;

import com.sequenceiq.flow.core.FlowEvent;

public enum StartDatahubFlowEvent implements FlowEvent {
    START_DATAHUB_EVENT("StartDatahubEvent"),
    START_DATAHUB_IN_PROGRESS_EVENT("StartDatahubInProgressEvent"),
    START_DATAHUB_SUCCESS_EVENT("StartDatahubSuccessEvent"),
    START_DATAHUB_FAILED_EVENT("StartDatahubFailedEvent"),
    START_DATAHUB_HANDLED_EVENT;

    private final String event;

    StartDatahubFlowEvent(String event) {
        this.event = event;
    }

    StartDatahubFlowEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
