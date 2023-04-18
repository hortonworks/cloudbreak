package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes;

import com.sequenceiq.flow.core.FlowEvent;

public enum DetermineDatalakeDataSizesEvent implements FlowEvent {
    DETERMINE_DATALAKE_DATA_SIZES_EVENT,
    DETERMINE_DATALAKE_DATA_SIZES_SALT_UPDATE_FINISHED_EVENT,
    DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_EVENT,
    DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT,
    DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_SUCCESS_EVENT,
    DETERMINE_DATALAKE_DATA_SIZES_FAILURE_HANDLED_EVENT;

    private final String event;

    DetermineDatalakeDataSizesEvent() {
        event = name();
    }

    @Override
    public String event() {
        return event;
    }
}
