package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class StackScaleTriggerEvent extends StackEvent {

    private final String instanceGroup;

    private final Integer adjustment;

    private final Set<String> hostNames;

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment) {
        this(selector, stackId, instanceGroup, adjustment, Collections.emptySet());
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, Promise<Boolean> accepted) {
        this(selector, stackId, instanceGroup, adjustment, Collections.emptySet(), accepted);
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, Set<String> hostNames) {
        super(selector, stackId);
        this.instanceGroup = instanceGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
    }

    public StackScaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, Set<String> hostNames, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
        this.instanceGroup = instanceGroup;
        this.adjustment = adjustment;
        this.hostNames = hostNames;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
