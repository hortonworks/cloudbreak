package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public class SshUserRequest<T> extends CloudPlatformRequest<T> {

    public SshUserRequest(CloudContext cloudContext) {
        super(cloudContext, null);
    }
}
