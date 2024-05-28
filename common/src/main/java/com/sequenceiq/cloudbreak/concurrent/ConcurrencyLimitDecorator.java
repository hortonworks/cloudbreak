package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public class ConcurrencyLimitDecorator implements Function<Callable, Callable> {

    private static final boolean FAIR = true;

    private final Semaphore runningSemaphore;

    public ConcurrencyLimitDecorator(int runningLimit) {
        this.runningSemaphore = new Semaphore(runningLimit, FAIR);
    }

    @Override
    public Callable apply(Callable callable) {
        return () -> {
            runningSemaphore.acquire();
            try {
                return callable.call();
            } finally {
                runningSemaphore.release();
            }
        };
    }
}
