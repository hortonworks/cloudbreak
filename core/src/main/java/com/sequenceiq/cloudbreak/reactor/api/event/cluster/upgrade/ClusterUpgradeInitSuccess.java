package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitSuccess extends StackEvent {
    @JsonCreator
    public ClusterUpgradeInitSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
