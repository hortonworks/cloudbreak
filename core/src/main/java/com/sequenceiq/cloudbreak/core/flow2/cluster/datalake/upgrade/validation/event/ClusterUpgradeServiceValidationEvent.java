package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeServiceValidationEvent extends StackEvent {

    private final boolean lockComponents;

    private final String targetRuntime;

    @JsonCreator
    public ClusterUpgradeServiceValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("lockComponents") boolean lockComponents,
            @JsonProperty("targetRuntime") String targetRuntime) {
        super(ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT.name(), resourceId);
        this.lockComponents = lockComponents;
        this.targetRuntime = targetRuntime;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public String getTargetRuntime() {
        return targetRuntime;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeServiceValidationEvent{" +
                "lockComponents=" + lockComponents +
                "targetRuntime=" + targetRuntime +
                "} " + super.toString();
    }
}
