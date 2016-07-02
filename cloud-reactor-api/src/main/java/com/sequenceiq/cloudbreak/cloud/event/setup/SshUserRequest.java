package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;

public class SshUserRequest<T> extends CloudPlatformRequest<T> {

    public SshUserRequest(CloudContext cloudContext) {
        super(cloudContext, null);
    }
}
