package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

public class StackDownscaleTriggerEvent extends StackScaleTriggerEvent {

    public StackDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
    }

    public StackDownscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Set<String> hostNames) {
        super(selector, stackId, instanceGroup, null, hostNames);
    }

}
