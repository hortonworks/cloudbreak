package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterManagerUpgradeRequest extends StackEvent {

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public ClusterManagerUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(stackId);
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }

    @Override public String toString() {
        return new StringJoiner(", ", ClusterManagerUpgradeRequest.class.getSimpleName() + "[", "]")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add(super.toString())
                .toString();
    }
}
