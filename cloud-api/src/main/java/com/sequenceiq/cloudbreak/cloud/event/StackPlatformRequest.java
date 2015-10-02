package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class StackPlatformRequest<T> extends CloudPlatformRequest<T> {

    private final CloudStack cloudStack;

    public StackPlatformRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(cloudContext, cloudCredential);
        this.cloudStack = cloudStack;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    @Override
    public String toString() {
        return "StackPlatformRequest{"
                + "cloudContext=" + getCloudContext()
                + ", cloudCredential=" + getCloudCredential()
                + ", cloudStack=" + cloudStack
                + '}';
    }
}
