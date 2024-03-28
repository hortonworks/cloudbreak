package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

import org.quartz.SchedulerConfigException;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.quartz.SimpleThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.internal.TimedRunnable;

/**
 * Custom ThreadPool and Executor implementation to provide metrics about the underlying threadpool.
 * <p>
 * This class implements the Executor interface to be able to use as a custom executor in Quartz. See: SchedulerFactoryBeanCustomizer.setTaskExecutor method
 * and LocalTimedExecutorThreadPool class.
 * This class implements the ThreadPool interface too to be able to properly delegate calls from the Quartz scheduler to the underlying
 * SimpleThreadPoolTaskExecutor implementation
 */
public class TimedSimpleThreadPoolTaskExecutor implements ThreadPool, Executor {

    private final MeterRegistry registry;

    private final ThreadPool delegate;

    private final Timer executionTimer;

    private final Timer idleTimer;

    public TimedSimpleThreadPoolTaskExecutor(MeterRegistry registry, ThreadPool delegate, String executorName, String metricPrefix,
            Iterable<Tag> tags) {
        Set<Class<?>> allowedClassed = Set.of(SimpleThreadPoolTaskExecutor.class, VirtualThreadPoolTaskExecutor.class);
        if (!allowedClassed.contains(delegate.getClass())) {
            throw new IllegalArgumentException(String.format("ThredPool delegate class be one of %s. Got type %s", allowedClassed, delegate.getClass()));
        }
        this.registry = registry;
        this.delegate = delegate;
        Tags finalTags = Tags.concat(tags, "name", executorName);
        this.executionTimer = registry.timer(metricPrefix + "executor", finalTags);
        this.idleTimer = registry.timer(metricPrefix + "executor.idle", finalTags);
        initThreadpoolMetrics(metricPrefix, finalTags);
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        return delegate.runInThread(new TimedRunnable(this.registry, this.executionTimer, this.idleTimer, runnable));
    }

    @Override
    public int blockForAvailableThreads() {
        return delegate.blockForAvailableThreads();
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        delegate.initialize();
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        delegate.shutdown(waitForJobsToComplete);
    }

    @Override
    public int getPoolSize() {
        return delegate.getPoolSize();
    }

    @Override
    public void setInstanceId(String schedInstId) {
        delegate.setInstanceId(schedInstId);
    }

    @Override
    public void setInstanceName(String schedName) {
        delegate.setInstanceName(schedName);
    }

    @Override
    public void execute(Runnable task) {
        Assert.notNull(task, "Runnable must not be null");
        if (!runInThread(task)) {
            throw new SchedulingException("Quartz SimpleThreadPool already shut down");
        }
    }

    private void initThreadpoolMetrics(String metricPrefix, Tags tags) {
        setThreadPoolMetric(metricProvider -> FunctionCounter.builder(metricPrefix + "executor.completed", delegate, metricProvider)
                .tags(tags)
                .description("The approximate total number of tasks that have completed execution")
                .baseUnit("tasks")
                .register(registry), SimpleThreadPool::getStartedTasks, VirtualThreadPoolTaskExecutor::getStartedTasks);
        setThreadPoolMetric(metricProvider -> Gauge.builder(metricPrefix + "executor.active", delegate, metricProvider)
                .tags(tags)
                .description("The approximate number of threads that are actively executing tasks")
                .baseUnit("threads")
                .register(registry), SimpleThreadPool::getBusyThreads, VirtualThreadPoolTaskExecutor::getBusyThreads);

        setThreadPoolMetric(metricProvider -> Gauge.builder(metricPrefix + "executor.pool.size", delegate, metricProvider)
                .tags(tags)
                .description("The current number of threads in the pool")
                .baseUnit("threads")
                .register(registry), SimpleThreadPool::getPoolSize, VirtualThreadPoolTaskExecutor::getPoolSize);
        setThreadPoolMetric(metricProvider -> Gauge.builder(metricPrefix + "executor.pool.core", delegate, metricProvider)
                .tags(tags)
                .description("The core number of threads for the pool")
                .baseUnit("threads")
                .register(registry), SimpleThreadPool::getPoolSize, VirtualThreadPoolTaskExecutor::getPoolSize);
        setThreadPoolMetric(metricProvider -> Gauge.builder(metricPrefix + "executor.pool.max", delegate, metricProvider)
                .tags(tags)
                .description("The maximum allowed number of threads in the pool")
                .baseUnit("threads")
                .register(registry), SimpleThreadPool::getPoolSize, VirtualThreadPoolTaskExecutor::getPoolSize);
    }

    private void setThreadPoolMetric(Consumer<ToDoubleFunction<ThreadPool>> metricFn,
            ToDoubleFunction<SimpleThreadPoolTaskExecutor> simpleThreadPoolMetric,
            ToDoubleFunction<VirtualThreadPoolTaskExecutor> virtualThreadPoolMetric) {
        if (delegate.getClass().equals(SimpleThreadPoolTaskExecutor.class)) {
            metricFn.accept(threadPool -> simpleThreadPoolMetric.applyAsDouble((SimpleThreadPoolTaskExecutor) threadPool));
        } else if (delegate.getClass().equals(VirtualThreadPoolTaskExecutor.class)) {
            metricFn.accept(threadPool -> virtualThreadPoolMetric.applyAsDouble((VirtualThreadPoolTaskExecutor) threadPool));
        }
    }
}
