package com.sequenceiq.freeipa.flow.stack.stop;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StopFreeIpaServicesEvent extends StackEvent {

    private final List<String> instanceIds;

    public StopFreeIpaServicesEvent(Long resourceId) {
        super(resourceId);
        this.instanceIds = List.of();
    }

    @JsonCreator
    public StopFreeIpaServicesEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(resourceId);
        this.instanceIds = instanceIds != null ? List.copyOf(instanceIds) : List.of();
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public String toString() {
        return "StopFreeIpaServicesEvent{instanceIds=" + instanceIds + "} " + super.toString();
    }
}
