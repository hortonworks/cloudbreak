package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent;

public class DetermineDatalakeDataSizesSubmissionEvent extends DetermineDatalakeDataSizesBaseEvent {
    private final String dataSizesResult;

    @JsonCreator
    public DetermineDatalakeDataSizesSubmissionEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("dataSizesResult") String dataSizesResult,
            @JsonProperty("operationId") String operationId) {
        super(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUBMISSION_EVENT.selector(), stackId, operationId);
        this.dataSizesResult = dataSizesResult;
    }

    public String getDataSizesResult() {
        return dataSizesResult;
    }
}
