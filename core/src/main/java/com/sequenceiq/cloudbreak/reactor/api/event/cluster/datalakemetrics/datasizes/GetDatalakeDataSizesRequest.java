package com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class GetDatalakeDataSizesRequest extends StackEvent {
    @JsonCreator
    public GetDatalakeDataSizesRequest(@JsonProperty("resourceId") Long stackId) {
        super(null, stackId);
    }
}
