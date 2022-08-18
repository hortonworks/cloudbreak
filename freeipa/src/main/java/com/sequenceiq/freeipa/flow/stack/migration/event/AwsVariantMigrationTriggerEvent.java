package com.sequenceiq.freeipa.flow.stack.migration.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

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

    @Override
    public String toString() {
        return "AwsVariantMigrationTriggerEvent{" +
                "hostGroupName='" + hostGroupName + '\'' +
                "} " + super.toString();
    }
}
