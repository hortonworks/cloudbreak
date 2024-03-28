package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.springframework.core.task.TaskDecorator;

public class TaskCounterDecorator implements Function<Callable, Callable>, TaskDecorator {

    private final AtomicLong activeCount;

    private final AtomicLong queuedCount;

    private final AtomicLong completedCount;

    public TaskCounterDecorator() {
        this.activeCount = new AtomicLong(0);
        this.queuedCount = new AtomicLong(0);
        this.completedCount = new AtomicLong(0);
    }

    @Override
    public Callable apply(Callable callable) {
        queuedCount.incrementAndGet();
        return () -> {
            try {
                queuedCount.decrementAndGet();
                activeCount.incrementAndGet();
                return callable.call();
            } finally {
                activeCount.decrementAndGet();
                completedCount.incrementAndGet();
            }
        };
    }

    public long getActiveCount() {
        return activeCount.get();
    }

    public long getQueuedCount() {
        return queuedCount.get();
    }

    public long getCompletedCount() {
        return completedCount.get();
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        queuedCount.incrementAndGet();
        return () -> {
            try {
                queuedCount.decrementAndGet();
                activeCount.incrementAndGet();
                runnable.run();
            } finally {
                activeCount.decrementAndGet();
                completedCount.incrementAndGet();
            }
        };
    }
}
