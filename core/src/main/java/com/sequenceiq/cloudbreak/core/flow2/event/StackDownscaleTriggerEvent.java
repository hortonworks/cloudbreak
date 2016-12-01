package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {
    private final Set<String> hostNames;

    public StackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
        this.hostNames = null;
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<String> hostNames) {
        super(selector, stackId, instanceGroup, null);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
