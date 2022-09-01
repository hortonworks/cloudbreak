package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class RdsUpgradeChainTriggerEvent extends StackEvent {

    private final TargetMajorVersion version;

    @JsonCreator
    public RdsUpgradeChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version) {
        super(selector, stackId);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "RdsUpgradeChainTriggerEvent{" +
                "version=" + version +
                "} " + super.toString();
    }
}
