package com.sequenceiq.cloudbreak.util.test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureTestImpl<T> implements Future<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureTestImpl.class);

    private Callable<T> resultProvider;

    public FutureTestImpl(Callable<T> resultProvider) {
        this.resultProvider = resultProvider;
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
        if (resultProvider == null) {
            return null;
        }
        try {
            return resultProvider.call();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        return get();
    }
}
