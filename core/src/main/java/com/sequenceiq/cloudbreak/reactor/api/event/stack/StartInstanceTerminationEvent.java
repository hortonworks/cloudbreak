package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.cloud.event.InstancePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartInstanceTerminationEvent extends StackEvent implements InstancePayload {
    private final String instanceId;

    public StartInstanceTerminationEvent(Long stackId, String instanceId) {
        this(null, stackId, instanceId);
    }

    public StartInstanceTerminationEvent(String selector, Long stackId, String instanceId) {
        super(selector, stackId);
        this.instanceId = instanceId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }
}
