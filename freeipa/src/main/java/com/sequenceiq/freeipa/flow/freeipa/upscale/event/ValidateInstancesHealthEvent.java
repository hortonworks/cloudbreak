package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateInstancesHealthEvent extends StackEvent {

    private final List<String> instanceIds;

    @JsonCreator
    public ValidateInstancesHealthEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds) {
        super(stackId);
        this.instanceIds = instanceIds;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public String toString() {
        return "ValidateInstancesEvent{" +
                "instanceIds=" + instanceIds +
                "} " + super.toString();
    }
}
