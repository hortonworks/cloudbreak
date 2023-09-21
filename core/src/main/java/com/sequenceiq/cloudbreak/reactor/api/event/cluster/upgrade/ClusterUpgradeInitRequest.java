package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeInitRequest extends StackEvent {

    @JsonCreator
    public ClusterUpgradeInitRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
