package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyDeregisterSuccess extends StackEvent {

    @JsonCreator
    public ClusterProxyDeregisterSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
