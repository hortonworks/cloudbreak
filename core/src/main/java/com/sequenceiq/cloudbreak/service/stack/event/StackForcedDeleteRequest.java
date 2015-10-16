package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

public class StackForcedDeleteRequest extends StackDeleteRequest {
    public StackForcedDeleteRequest(CloudPlatform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }
}
