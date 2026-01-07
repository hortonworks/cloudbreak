package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClusterManagerUpgradeRequest extends AbstractClusterUpgradeEvent {

    private final boolean rollingUpgradeEnabled;

    private final String targetRuntimeVersion;

    @JsonCreator
    public ClusterManagerUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("rollingUpgradeEnabled") boolean rollingUpgradeEnabled,
            @JsonProperty("targetRuntimeVersion") String targetRuntimeVersion) {
        super(stackId, upgradeCandidateProducts);
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
        this.targetRuntimeVersion = targetRuntimeVersion;
    }

    public boolean isRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public String getTargetRuntimeVersion() {
        return targetRuntimeVersion;
    }

    @Override
    public String selector() {
        return "ClusterManagerUpgradeRequest";
    }

    @Override
    public String toString() {
        return "ClusterManagerUpgradeRequest{" +
                "rollingUpgradeEnabled=" + rollingUpgradeEnabled +
                ", targetRuntimeVersion='" + targetRuntimeVersion + '\'' +
                "} " + super.toString();
    }
}
