package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetDatalakeDataSizesRequest extends DetermineDatalakeDataSizesBaseEvent {
    @JsonCreator
    public GetDatalakeDataSizesRequest(@JsonProperty("resourceId") Long stackId, @JsonProperty("operationId") String operationId) {
        super(null, stackId, operationId);
    }
}
