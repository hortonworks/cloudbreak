package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeValidationEvent extends StackEvent {

    // TODO CB-33421: Remove once in-flight flow events no longer depend on imageId in JSON.
    private final String imageId;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ClusterUpgradeValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(selector, resourceId);
        // TODO CB-33421: Remove imageId field once in-flight flow events no longer depend on it in JSON.
        this.imageId = imageId;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public String getImageId() {
        // TODO CB-33421: Remove imageId field once in-flight flow events no longer depend on it in JSON.
        return clusterUpgradeProperties != null ? clusterUpgradeProperties.getTargetImageId() : imageId;
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeValidationEvent{" +
                "imageId='" + getImageId() + '\'' +
                ", clusterUpgradeProperties=" + clusterUpgradeProperties +
                "} " + super.toString();
    }
}
