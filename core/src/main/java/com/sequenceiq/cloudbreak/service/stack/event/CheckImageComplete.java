package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class CheckImageComplete extends ProvisionEvent {

    public CheckImageComplete(Platform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
