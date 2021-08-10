package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationEvent extends StackEvent {

    private final String imageId;

    public ClusterUpgradeValidationEvent(String selector, Long resourceId, String imageId) {
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
