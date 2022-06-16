package com.sequenceiq.cloudbreak.reactor.api.event.cluster.atlas;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckAtlasUpdatedSaltSuccessEvent extends CheckAtlasUpdatedStackEvent {
    @JsonCreator
    public CheckAtlasUpdatedSaltSuccessEvent(@JsonProperty("resourceId") Long stackId) {
        super(null, stackId);
    }
}
