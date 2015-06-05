package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;

import reactor.rx.Promise;

public class CloudPlatformRequest<T> {

    private StackContext stackContext;

    private Promise<T> result;

    public CloudPlatformRequest(StackContext stackContext, Promise<T> result) {
        this.stackContext = stackContext;
        this.result = result;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public StackContext getStackContext() {
        return stackContext;
    }

    public Promise<T> getResult() {
        return result;
    }
}
