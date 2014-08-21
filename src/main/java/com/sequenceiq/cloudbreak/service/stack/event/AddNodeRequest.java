package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AddNodeRequest extends ProvisionEvent {
    public AddNodeRequest(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }
}
