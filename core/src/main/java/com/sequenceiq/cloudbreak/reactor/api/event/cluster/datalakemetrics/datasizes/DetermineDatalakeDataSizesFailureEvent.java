package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DetermineDatalakeDataSizesFailureEvent extends DetermineDatalakeDataSizesBaseEvent {
    private final Exception exception;

    @JsonCreator
    public DetermineDatalakeDataSizesFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT.event(), stackId, null);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
