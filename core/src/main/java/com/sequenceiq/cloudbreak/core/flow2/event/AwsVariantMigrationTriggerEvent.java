package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AwsVariantMigrationTriggerEvent extends StackEvent {

    private final String hostGroupName;

    @JsonCreator
    public AwsVariantMigrationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupName") String hostGroupName) {
        super(selector, stackId);
        this.hostGroupName = hostGroupName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
