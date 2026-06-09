package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ClusterUpgradeImageValidationFinishedEvent extends ClusterUpgradeValidationEvent {

    private final long requiredFreeSpace;

    private final Set<String> warningMessages;

    @JsonCreator
    public ClusterUpgradeImageValidationFinishedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties,
            @JsonProperty("requiredFreeSpace") long requiredFreeSpace,
            @JsonProperty("warningMessages") Set<String> warningMessages) {
        super(selector, resourceId, imageId, clusterUpgradeProperties);
        this.requiredFreeSpace = requiredFreeSpace;
        this.warningMessages = warningMessages;
    }

    public long getRequiredFreeSpace() {
        return requiredFreeSpace;
    }

    public Set<String> getWarningMessages() {
        return warningMessages;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeImageValidationFinishedEvent{" +
                "requiredFreeSpace=" + requiredFreeSpace +
                ", warningMessages=" + warningMessages +
                "} " + super.toString();
    }
}
