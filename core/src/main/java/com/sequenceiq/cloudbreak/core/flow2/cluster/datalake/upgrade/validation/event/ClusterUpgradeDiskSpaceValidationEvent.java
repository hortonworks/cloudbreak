package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeDiskSpaceValidationEvent extends StackEvent {

    private final long requiredFreeSpace;

    @JsonCreator
    public ClusterUpgradeDiskSpaceValidationEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("requiredFreeSpace") long requiredFreeSpace) {
        super(selector, resourceId);
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
