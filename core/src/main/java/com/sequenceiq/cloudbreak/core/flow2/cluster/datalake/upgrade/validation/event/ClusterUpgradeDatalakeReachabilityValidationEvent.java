package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeDatalakeReachabilityValidationEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeDatalakeReachabilityValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_DATALAKE_REACHABILITY_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
    }
}
