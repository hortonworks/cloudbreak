package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class InstanceTerminationTriggerEvent extends StackEvent implements InstancePayload {
    private final String instanceId;

    public InstanceTerminationTriggerEvent(String selector, Long stackId, String instanceId) {
        super(selector, stackId);
        this.instanceId = instanceId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
}
