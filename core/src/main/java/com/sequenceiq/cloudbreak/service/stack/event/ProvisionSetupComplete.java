package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ProvisionSetupComplete extends ProvisionEvent {

    public ProvisionSetupComplete(Platform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
