package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_S3GUARD_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeS3guardValidationEvent extends StackEvent {

    private final String imageId;

    @JsonCreator
    public ClusterUpgradeS3guardValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId) {
        super(VALIDATE_S3GUARD_EVENT.name(), resourceId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeS3guardValidationEvent{" +
                "imageId='" + imageId + '\'' +
                "} " + super.toString();
    }
}