package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class RemoveInstanceRequest extends ProvisionEvent {
    private final String instanceId;

    public RemoveInstanceRequest(Platform cloudPlatform, Long stackId, String instanceId) {
        super(cloudPlatform, stackId);
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
