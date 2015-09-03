package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisionSetupComplete extends ProvisionEvent {

    public ProvisionSetupComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
