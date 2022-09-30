package com.sequenceiq.datalake.flow.dr.datalakeinfo;

import com.sequenceiq.flow.core.FlowEvent;

public enum SubmitDatalakeDataInfoEvent implements FlowEvent {
    SUBMIT_DATALAKE_DATA_INFO_EVENT(),
    SUBMIT_DATALAKE_DATA_INFO_FAILED_EVENT(),
    SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT(),
    SUBMIT_DATALAKE_DATA_INFO_SUCCESS_EVENT();

    private final String event;

    SubmitDatalakeDataInfoEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
