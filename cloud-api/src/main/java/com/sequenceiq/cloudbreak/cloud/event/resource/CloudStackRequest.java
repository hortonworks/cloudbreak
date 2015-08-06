package com.sequenceiq.cloudbreak.cloud.event.resource;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class CloudStackRequest<T> extends CloudPlatformRequest<T> {

    private CloudStack cloudStack;

    public CloudStackRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, Promise<T> result) {
        super(cloudContext, cloudCredential, result);
        this.cloudStack = cloudStack;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudStackRequest{" +
                ", cloudStack=" + cloudStack +
                '}';
    }
    //END GENERATED CODE
}
