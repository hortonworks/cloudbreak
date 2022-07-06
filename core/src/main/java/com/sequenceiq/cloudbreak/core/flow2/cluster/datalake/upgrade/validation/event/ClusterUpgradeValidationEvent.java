package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationEvent extends StackEvent {

    private final String imageId;

    @JsonCreator
    public ClusterUpgradeValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId) {
        super(selector, resourceId);
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeValidationEvent{" +
                "imageId='" + imageId + '\'' +
                "} " + super.toString();
    }
}
