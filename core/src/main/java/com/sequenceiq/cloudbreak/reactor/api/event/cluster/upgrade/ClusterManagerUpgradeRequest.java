package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {

    private final boolean runtimeServicesStartNeeded;

    @JsonCreator
    public ClusterManagerUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("runtimeServicesStartNeeded") boolean runtimeServicesStartNeeded) {
        super(stackId);
        this.runtimeServicesStartNeeded = runtimeServicesStartNeeded;
    }

    public boolean isRuntimeServicesStartNeeded() {
        return runtimeServicesStartNeeded;
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }
}
