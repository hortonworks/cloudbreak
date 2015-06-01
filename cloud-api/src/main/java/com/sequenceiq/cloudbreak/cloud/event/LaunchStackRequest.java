package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

import reactor.rx.Promise;

public class LaunchStackRequest<T> extends CloudPlatformRequest<T> {

    private CloudCredential cloudCredential;

    private CloudStack cloudStack;

    public LaunchStackRequest(StackContext stackContext, CloudCredential cloudCredential, CloudStack cloudStack, Promise<T> result) {
        super(stackContext, result);
        this.cloudCredential = cloudCredential;
        this.cloudStack = cloudStack;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    @Override
    public String toString() {
        return "LaunchStackRequest{" +
                "cloudCredential=" + cloudCredential +
                ", cloudStack=" + cloudStack +
                '}';
    }
}
