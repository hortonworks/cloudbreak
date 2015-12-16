package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ProvisionRequest extends ProvisionEvent {

    public ProvisionRequest(Platform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
