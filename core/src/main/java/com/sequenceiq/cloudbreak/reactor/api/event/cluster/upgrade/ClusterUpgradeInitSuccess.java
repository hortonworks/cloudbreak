package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.common.model.OsType;

public class ClusterUpgradeInitSuccess extends AbstractClusterUpgradeEvent {

    @JsonCreator
    public ClusterUpgradeInitSuccess(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("upgradeCandidateProducts") Set<ClouderaManagerProduct> upgradeCandidateProducts,
            @JsonProperty("originalOsType") OsType originalOsType) {
        super(stackId, upgradeCandidateProducts, originalOsType);
    }
}
