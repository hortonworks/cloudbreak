package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeFreeIpaStatusValidationEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeFreeIpaStatusValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_FREEIPA_STATUS_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
    }
}
