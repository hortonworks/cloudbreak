package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeDiskSpaceValidationEvent extends StackEvent {

    private final long requiredFreeSpace;

    public ClusterUpgradeDiskSpaceValidationEvent(String selector, Long resourceId, long requiredFreeSpace) {
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
