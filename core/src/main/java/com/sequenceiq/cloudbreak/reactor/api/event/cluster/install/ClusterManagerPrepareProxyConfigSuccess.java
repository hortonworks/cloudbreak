package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerPrepareProxyConfigSuccess extends StackEvent {
    @JsonCreator
    public ClusterManagerPrepareProxyConfigSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
