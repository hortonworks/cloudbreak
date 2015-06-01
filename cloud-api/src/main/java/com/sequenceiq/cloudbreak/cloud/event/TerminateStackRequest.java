package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import reactor.rx.Promise;

public class TerminateStackRequest<T> extends CloudPlatformRequest<T> {

    private CloudCredential cloudCredential;

    public TerminateStackRequest(StackContext stackContext, CloudCredential cloudCredential, Promise<T> result) {
        super(stackContext, result);
        this.cloudCredential = cloudCredential;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "TerminateStackRequest{" +
                "cloudCredential=" + cloudCredential +
                '}';
    }
    //END GENERATED CODE
}
