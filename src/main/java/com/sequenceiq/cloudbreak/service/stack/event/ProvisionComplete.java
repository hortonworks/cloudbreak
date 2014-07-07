package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionComplete extends ProvisionEvent {

    public ProvisionComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
