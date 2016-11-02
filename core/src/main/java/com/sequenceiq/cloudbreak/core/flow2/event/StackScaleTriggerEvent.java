package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackScaleTriggerEvent extends StackEvent {
    private final String instanceGroup;

    private final Integer adjustment;

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment) {
        super(selector, stackId);
        this.instanceGroup = instanceGroup;
        this.adjustment = adjustment;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
