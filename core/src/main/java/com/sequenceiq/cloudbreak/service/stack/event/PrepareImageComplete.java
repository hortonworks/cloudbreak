package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class PrepareImageComplete extends ProvisionEvent {

    public PrepareImageComplete(Platform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
