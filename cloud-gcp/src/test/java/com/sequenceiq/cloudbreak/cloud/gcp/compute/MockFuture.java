package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MockFuture implements Future<Void> {

    private Callable<Void> callable;

    public MockFuture(Callable<Void> callable) {
        this.callable = callable;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Void get() {
        Void call = null;
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void get(long timeout, TimeUnit unit) {
        return null;
    }
}
