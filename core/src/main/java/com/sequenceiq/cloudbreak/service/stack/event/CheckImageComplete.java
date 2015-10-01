package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

public class CheckImageComplete extends ProvisionEvent {

    public CheckImageComplete(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }

}
