package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeDiskSpaceValidationEvent extends ClusterUpgradeValidationEvent {

    private final long requiredFreeSpace;

    @JsonCreator
    public ClusterUpgradeDiskSpaceValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties,
            @JsonProperty("requiredFreeSpace") long requiredFreeSpace) {
        super(selector, resourceId, imageId, clusterUpgradeProperties);
        this.requiredFreeSpace = requiredFreeSpace;
    }

    public long getRequiredFreeSpace() {
        return requiredFreeSpace;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeDiskSpaceValidationEvent{" +
                "requiredFreeSpace=" + requiredFreeSpace +
                "} " + super.toString();
    }
}
