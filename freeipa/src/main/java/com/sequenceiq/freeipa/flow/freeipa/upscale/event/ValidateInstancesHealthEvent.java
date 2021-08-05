package com.sequenceiq.freeipa.flow.freeipa.upscale.event;

import java.util.List;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ValidateInstancesHealthEvent extends StackEvent {

    private final List<String> instanceIds;

    public ValidateInstancesHealthEvent(Long stackId, List<String> instanceIds) {
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
