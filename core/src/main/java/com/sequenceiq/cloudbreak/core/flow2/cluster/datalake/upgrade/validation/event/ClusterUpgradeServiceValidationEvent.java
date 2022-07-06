package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeServiceValidationEvent extends StackEvent {

    private final boolean lockComponents;

    @JsonCreator
    public ClusterUpgradeServiceValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("lockComponents") boolean lockComponents) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT.name(), resourceId);
        this.lockComponents = lockComponents;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeServiceValidationEvent{" +
                "lockComponents=" + lockComponents +
                "} " + super.toString();
    }
}
