package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeS3guardValidationFinishedEvent  extends StackEvent {

    private final String imageId;

    @JsonCreator
    public ClusterUpgradeS3guardValidationFinishedEvent(@JsonProperty("resourceId") Long stackId,
            @JsonProperty("imageId") String imageId) {
        super(START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.name(), stackId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeS3guardValidationFinishedEvent{" +
                "imageId='" + imageId + '\'' +
                "} " + super.toString();
    }
}