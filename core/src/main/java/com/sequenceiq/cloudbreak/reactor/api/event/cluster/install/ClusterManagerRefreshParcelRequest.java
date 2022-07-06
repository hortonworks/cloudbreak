package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerRefreshParcelRequest extends StackEvent {
    @JsonCreator
    public ClusterManagerRefreshParcelRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
