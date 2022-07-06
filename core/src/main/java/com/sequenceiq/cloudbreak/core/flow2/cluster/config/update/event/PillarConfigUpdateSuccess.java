package com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PillarConfigUpdateSuccess extends StackEvent {

    @JsonCreator
    public PillarConfigUpdateSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
