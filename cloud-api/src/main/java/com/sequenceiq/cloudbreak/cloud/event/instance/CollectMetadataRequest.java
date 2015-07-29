package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class CollectMetadataRequest<T> extends CloudPlatformRequest<T> {

    private CloudCredential cloudCredential;

    private CloudStack cloudStack;

    public CollectMetadataRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack, Promise<T> result) {
        super(cloudContext, result);
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudStackRequest{" +
                "cloudCredential=" + cloudCredential +
                ", cloudStack=" + cloudStack +
                '}';
    }
    //END GENERATED CODE

}
