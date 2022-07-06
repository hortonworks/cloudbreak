package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartClusterSuccess extends StackEvent {
    public StartClusterSuccess(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public StartClusterSuccess(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
