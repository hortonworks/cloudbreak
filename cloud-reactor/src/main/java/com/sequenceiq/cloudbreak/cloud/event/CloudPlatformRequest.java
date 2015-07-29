package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

import reactor.rx.Promise;

public class CloudPlatformRequest<T> {

    private CloudContext cloudContext;

    private Promise<T> result;

    public CloudPlatformRequest(CloudContext cloudContext, Promise<T> result) {
        this.cloudContext = cloudContext;
        this.result = result;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Promise<T> getResult() {
        return result;
    }
}
