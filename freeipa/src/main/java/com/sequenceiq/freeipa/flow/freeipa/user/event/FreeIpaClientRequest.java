package com.sequenceiq.freeipa.flow.freeipa.user.event;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class FreeIpaClientRequest<T> implements Selectable {

    private final Long stackId;

    private final Promise<T> result;

    public FreeIpaClientRequest(Long stackId) {
        this.stackId = requireNonNull(stackId);
        result = Promise.prepare();
    }

    public static String selector(Class<?> clazz) {
        return clazz.getSimpleName().toUpperCase(Locale.ROOT);
    }

    @Override
    public String selector() {
        return selector(getClass());
    }

    @Override
    public Long getResourceId() {
        return stackId;
    }

    public Promise<T> getResult() {
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
        return "FreeIpaRequest{"
                + "stackId=" + stackId
                + '}';
    }
}
