package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeFreeIpaStatusValidationFinishedEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeFreeIpaStatusValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_SERVICE_VALIDATION_EVENT.name(), resourceId, imageId,
                clusterUpgradeProperties);
    }
}
