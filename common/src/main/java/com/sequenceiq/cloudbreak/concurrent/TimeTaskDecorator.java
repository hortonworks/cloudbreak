package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.core.task.TaskDecorator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.internal.TimedCallable;
import io.micrometer.core.instrument.internal.TimedRunnable;

public class TimeTaskDecorator implements TaskDecorator, Function<Callable, Callable> {

    private static final String METRIC_PREFIX = "threadpool.";

    private final MeterRegistry registry;

    private final Timer executionTimer;

    private final Timer idleTimer;

    public TimeTaskDecorator(MeterRegistry registry, String executorServiceName) {
        this.registry = registry;
        Tags finalTags = Tags.of(new String[] {"name", executorServiceName});
        this.executionTimer = registry.timer(METRIC_PREFIX + "executor", finalTags);
        this.idleTimer = registry.timer(METRIC_PREFIX + "executor.idle", finalTags);
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return new TimedRunnable(registry, executionTimer, idleTimer, runnable);
    }

    @Override
    public Callable apply(Callable callable) {
        return new TimedCallable(registry, executionTimer, idleTimer, callable);
    }
}
