package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeRequest extends StackEvent {

    private final boolean patchUpgrade;

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public ClusterUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("patchUpgrade") boolean patchUpgrade,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(stackId);
        this.patchUpgrade = patchUpgrade;
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public boolean isPatchUpgrade() {
        return patchUpgrade;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeRequest.class.getSimpleName() + "[", "]")
                .add("patchUpgrade=" + patchUpgrade)
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add(super.toString())
                .toString();
    }
}
