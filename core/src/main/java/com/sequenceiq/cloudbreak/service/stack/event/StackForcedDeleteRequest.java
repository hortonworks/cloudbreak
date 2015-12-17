package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class StackForcedDeleteRequest extends StackDeleteRequest {
    public StackForcedDeleteRequest(Platform cloudPlatform, Long stackId) {
        super(cloudPlatform, stackId);
    }
}
