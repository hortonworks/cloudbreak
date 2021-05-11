package com.sequenceiq.cloudbreak.cloud.event;

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.rx.Promise;
import reactor.rx.Promises;

public class CloudPlatformRequest<T> implements Selectable {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final Promise<T> result;

    public CloudPlatformRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        result = Promises.prepare();
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    @Override
    public String selector() {
        return selector(getClass());
    }

    @Override
    public Long getResourceId() {
        if (cloudContext != null) {
            return cloudContext.getId();
        }
        return null;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public Subscriber<T> getResult() {
        return result;
    }

    public T await() throws InterruptedException {
        return await(1, TimeUnit.HOURS);
    }

    public T await(long timeout, TimeUnit unit) throws InterruptedException {
        T result = this.result.await(timeout, unit);
        if (result == null) {
            throw new InterruptedException("Operation timed out, couldn't retrieve result");
        }
        return result;
    }

    @Override
    public String toString() {
        return "CloudPlatformRequest{"
                + "cloudContext=" + cloudContext
                + '}';
    }
}
