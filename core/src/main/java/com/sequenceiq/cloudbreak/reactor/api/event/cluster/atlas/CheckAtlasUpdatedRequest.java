package com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckAtlasUpdatedRequest extends CheckAtlasUpdatedStackEvent {
    @JsonCreator
    public CheckAtlasUpdatedRequest(@JsonProperty("resourceId") Long stackId) {
        super(null, stackId);
    }
}
