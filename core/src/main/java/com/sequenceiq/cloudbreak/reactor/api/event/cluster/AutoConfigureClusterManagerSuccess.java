package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AutoConfigureClusterManagerSuccess extends StackEvent {
    @JsonCreator
    public AutoConfigureClusterManagerSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
