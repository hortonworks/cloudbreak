package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class InstanceTerminationTriggerEvent extends StackEvent implements InstancePayload {
    private final Set<String> instanceIds;

    public InstanceTerminationTriggerEvent(String selector, Long stackId, Set<String> instanceIds) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
    }

    @Override
    public Set<String> getInstanceIds() {
        return instanceIds;
    }
}
