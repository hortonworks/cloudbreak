package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClusterManagerUpgradeRequest extends AbstractClusterUpgradeEvent {

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public ClusterManagerUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(stackId, upgradeCandidateProducts);
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterManagerUpgradeRequest.class.getSimpleName() + "[", "]")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add(super.toString())
                .toString();
    }
}
