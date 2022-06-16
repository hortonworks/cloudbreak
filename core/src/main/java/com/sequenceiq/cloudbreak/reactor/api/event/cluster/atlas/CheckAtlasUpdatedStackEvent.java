package com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class CheckAtlasUpdatedStackEvent extends StackEvent {
    @JsonCreator
    public CheckAtlasUpdatedStackEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
