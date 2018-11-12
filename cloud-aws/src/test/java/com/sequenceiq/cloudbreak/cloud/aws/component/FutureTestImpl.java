package com.sequenceiq.cloudbreak.cloud.aws.component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureTestImpl<T> implements Future<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureTestImpl.class);

    private T result;

    public FutureTestImpl(T result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        LOGGER.info("cancel called - true");
        return true;
    }

    @Override
    public boolean isCancelled() {
        LOGGER.info("isCancelled called - false");
        return false;
    }

    @Override
    public boolean isDone() {
        LOGGER.info("isDone called - true");
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        LOGGER.info("get called - returning result");
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        LOGGER.info("get called - returning result");
        return result;
    }
}
