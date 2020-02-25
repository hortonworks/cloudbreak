package com.sequenceiq.periscope.component;

import java.util.function.Supplier;

import com.sequenceiq.cloudbreak.common.service.TransactionExecutorService;

public class TransactionExecutionServiceTest implements TransactionExecutorService {
    public synchronized <T> T required(Supplier<T> callback) {
        return callback.get();
    }

    public synchronized <T> T requiresNew(Supplier<T> callback) {
        return callback.get();
    }

    public synchronized <T> T mandatory(Supplier<T> callback) {
        return callback.get();
    }

    public <T> T supports(Supplier<T> callback) {
        return callback.get();
    }

    public <T> T notSupported(Supplier<T> callback) {
        return callback.get();
    }

    public <T> T never(Supplier<T> callback) {
        return callback.get();
    }

    @Override
    public void required(Runnable callback) {
        callback.run();
    }

    @Override
    public void requiresNew(Runnable callback) {
        callback.run();
    }

    @Override
    public void mandatory(Runnable callback) {
        callback.run();
    }

    @Override
    public void supports(Runnable callback) {
        callback.run();
    }

    @Override
    public void notSupported(Runnable callback) {
        callback.run();
    }

    @Override
    public void never(Runnable callback) {
        callback.run();
    }

}
