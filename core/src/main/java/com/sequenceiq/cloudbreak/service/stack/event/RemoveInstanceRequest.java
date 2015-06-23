package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class RemoveInstanceRequest extends ProvisionEvent {
    private final String instanceId;

    public RemoveInstanceRequest(CloudPlatform cloudPlatform, Long stackId, String instanceId) {
        super(cloudPlatform, stackId);
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
