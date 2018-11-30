package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import reactor.rx.Promise;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {

    private final Set<Long> privateIds;

    public StackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
        privateIds = null;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<Long> privateIds) {
        super(selector, stackId, instanceGroup, null);
        this.privateIds = privateIds;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<Long> privateIds, Promise<Boolean> accepted) {
        super(selector, stackId, instanceGroup, null, accepted);
        this.privateIds = privateIds;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }
}
