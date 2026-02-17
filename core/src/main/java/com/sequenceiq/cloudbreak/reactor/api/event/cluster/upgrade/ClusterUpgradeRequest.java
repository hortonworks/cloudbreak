package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.common.model.OsType;

public class ClusterUpgradeRequest extends AbstractClusterUpgradeEvent {

    private final boolean rollingUpgradeEnabled;

    @JsonCreator
    public ClusterUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("originalOsType") OsType originalOsType) {
        super(stackId, upgradeCandidateProducts, originalOsType);
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeRequest.class.getSimpleName() + "[", "]")
                .add("rollingUpgradeEnabled=" + rollingUpgradeEnabled)
                .add(super.toString())
                .toString();
    }
}
