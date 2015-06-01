package com.sequenceiq.cloudbreak.cloud.event;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;

import reactor.rx.Promise;

public class CloudPlatformRequest {

    private StackContext stackContext;

    private Promise<String> result;

    public CloudPlatformRequest(StackContext stackContext) {
        this.stackContext = stackContext;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public StackContext getStackContext() {
        return stackContext;
    }

    public Promise<String> getResult() {
        return result;
    }

    public void setResult(Promise<String> result) {
        this.result = result;
    }
}
