package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeImageValidationFinishedEvent extends StackEvent {

    private final long requiredFreeSpace;

    @JsonCreator
    public ClusterUpgradeImageValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("requiredFreeSpace") long requiredFreeSpace) {
        super(FINISH_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.selector(), resourceId);
        this.requiredFreeSpace = requiredFreeSpace;
    }

    public long getRequiredFreeSpace() {
        return requiredFreeSpace;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeImageValidationFinishedEvent{" +
                "requiredFreeSpace=" + requiredFreeSpace +
                "} " + super.toString();
    }
}
