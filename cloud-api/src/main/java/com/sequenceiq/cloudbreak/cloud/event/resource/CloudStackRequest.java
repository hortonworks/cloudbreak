package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public class CloudStackRequest<T> extends CloudPlatformRequest<T> {

    private CloudStack cloudStack;

    public CloudStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        super(cloudContext, cloudCredential);
        this.cloudStack = cloudStack;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    @Override
    public String toString() {
        return "CloudStackRequest{" +
                ", cloudStack=" + cloudStack +
                '}';
    }
}
