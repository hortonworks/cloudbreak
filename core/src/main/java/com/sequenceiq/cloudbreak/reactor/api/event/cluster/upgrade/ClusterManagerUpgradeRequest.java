package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {

    @JsonCreator
    public ClusterManagerUpgradeRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }
}
