package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeDiskSpaceValidationFinishedEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeDiskSpaceValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_CLOUDPROVIDER_CHECK_UPDATE_EVENT.name(), resourceId, imageId,
                clusterUpgradeProperties);
    }
}
