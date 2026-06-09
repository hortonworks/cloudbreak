package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeS3guardValidationFinishedEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeS3guardValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
    }

    @Override
    public String toString() {
        return "ClusterUpgradeS3guardValidationFinishedEvent{} " + super.toString();
    }
}
