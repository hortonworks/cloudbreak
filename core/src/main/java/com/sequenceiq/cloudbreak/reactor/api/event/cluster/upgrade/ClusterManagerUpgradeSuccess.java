package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.common.model.OsType;

public class ClusterManagerUpgradeSuccess extends AbstractClusterUpgradeEvent {

    @JsonCreator
    public ClusterManagerUpgradeSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("originalOsType") OsType originalOsType) {
        super(stackId, upgradeCandidateProducts, originalOsType);
    }

    @Override
    public String selector() {
        return CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT.event();
    }

    @Override
    public String toString() {
        return "ClusterManagerUpgradeSuccess{} " + super.toString();
    }
}
