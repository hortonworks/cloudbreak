package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PillarConfigUpdateRequest extends StackEvent {

    @JsonCreator
    public PillarConfigUpdateRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
