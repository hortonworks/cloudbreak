package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import reactor.rx.Promise;

public class CloudPlatformRequest<T> {

    private CloudContext cloudContext;

    private CloudCredential cloudCredential;

    private Promise<T> result;

    public CloudPlatformRequest(CloudContext cloudContext, CloudCredential cloudCredential, Promise<T> result) {
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.result = result;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public String selector() {
        return selector(getClass());
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public Promise<T> getResult() {
        return result;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "CloudPlatformRequest{" +
                "cloudContext=" + cloudContext +
                ", cloudCredential=" + cloudCredential +
                '}';
    }
    //END GENERATED CODE
}
