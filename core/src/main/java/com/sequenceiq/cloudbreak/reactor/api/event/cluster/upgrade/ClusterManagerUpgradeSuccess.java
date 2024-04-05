package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClusterManagerUpgradeSuccess extends AbstractClusterUpgradeEvent {

    @JsonCreator
    public ClusterManagerUpgradeSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts) {
        super(stackId, upgradeCandidateProducts);
    }

    @Override
    public String selector() {
        return CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT.event();
    }

}
