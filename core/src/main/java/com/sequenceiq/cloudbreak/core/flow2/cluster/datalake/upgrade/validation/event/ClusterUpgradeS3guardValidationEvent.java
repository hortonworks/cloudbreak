package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_S3GUARD_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeS3guardValidationEvent extends ClusterUpgradeValidationEvent {

    @JsonCreator
    public ClusterUpgradeS3guardValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(VALIDATE_S3GUARD_EVENT.name(), resourceId, imageId, clusterUpgradeProperties);
    }

    @Override
    public String toString() {
        return "ClusterUpgradeS3guardValidationEvent{} " + super.toString();
    }
}
