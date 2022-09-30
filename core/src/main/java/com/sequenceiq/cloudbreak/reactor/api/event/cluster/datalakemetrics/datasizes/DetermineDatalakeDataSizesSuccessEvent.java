package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DetermineDatalakeDataSizesSuccessEvent extends StackEvent {
    private final String dataSizesResult;

    @JsonCreator
    public DetermineDatalakeDataSizesSuccessEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("dataSizesResult") String dataSizesResult) {
        super(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_SUCCESS_EVENT.selector(), stackId);
        this.dataSizesResult = dataSizesResult;
    }

    public String getDataSizesResult() {
        return dataSizesResult;
    }
}
