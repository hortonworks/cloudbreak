package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClusterUpgradeRequest extends AbstractClusterUpgradeEvent {

    @Deprecated
    private final boolean patchUpgrade;

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public ClusterUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("patchUpgrade") boolean patchUpgrade,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled) {
        super(stackId, upgradeCandidateProducts);
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
