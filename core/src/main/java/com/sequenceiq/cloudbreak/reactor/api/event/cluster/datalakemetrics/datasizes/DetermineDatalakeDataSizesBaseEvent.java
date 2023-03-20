package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DetermineDatalakeDataSizesBaseEvent extends StackEvent {
    private final String operationId;

    @JsonCreator
    public DetermineDatalakeDataSizesBaseEvent(@JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }
}
