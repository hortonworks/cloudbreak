package com.sequenceiq.cloudbreak.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.CompositeTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Component
public class CommonExecutorServiceFactory {

    private static final String METRIC_PREFIX = "threadpool.";

    @Inject
    private MeterRegistry meterRegistry;

    // CHECKSTYLE:OFF
    public ExecutorService newThreadPoolExecutorService(String namePrefix, String executorServiceName, int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler rejectedExecutionHandler,
            List<Function<Callable, Callable>> decorators) {
        // CHECKSTYLE:ON
        ExecutorService threadPoolExecutor = monitor(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Thread.ofPlatform().name(namePrefix + "-", 0).daemon().factory(), rejectedExecutionHandler), executorServiceName);
        return decorate(threadPoolExecutor, decorators);
    }

    public ExecutorService newVirtualThreadExecutorService(String namePrefix, String executorServiceName, List<Function<Callable, Callable>> decorators) {
        List<Function<Callable, Callable>> extendedDecorators = new ArrayList<>(decorators);
        TaskCounterDecorator taskCounterDecorator = new TaskCounterDecorator();
        extendedDecorators.add(taskCounterDecorator);
        return monitorVirtual(decorate(newVirtualThreadExecutorService(namePrefix), extendedDecorators), taskCounterDecorator, executorServiceName);
    }

    public AsyncTaskExecutor newAsyncTaskExecutor(String executorServiceName, boolean virtualThread, int corePoolSize, int queueCapacity,
            int awaitTerminationSeconds) {
        CompositeTaskDecorator taskDecorator = new CompositeTaskDecorator(List.of(
                new MDCCopyDecorator(),
                new ThreadBasedUserCrnDecorator(),
                new TimeTaskDecorator(meterRegistry, executorServiceName)));
        if (virtualThread) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
            executor.setVirtualThreads(true);
            executor.setThreadNamePrefix(executorServiceName + "-");
            executor.setTaskDecorator(taskDecorator);
            return executor;
        } else {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(corePoolSize);
            executor.setQueueCapacity(queueCapacity);
            executor.setThreadNamePrefix(executorServiceName + "-");
            executor.setTaskDecorator(taskDecorator);
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
            executor.initialize();
            return executor;
        }
    }

    public void monitorTaskCount(TaskCounterDecorator taskCounterDecorator, String executorServiceName) {
        Tags tags = Tags.of(new String[] {"name", executorServiceName});
        FunctionCounter.builder(METRIC_PREFIX + "executor.completed", taskCounterDecorator, TaskCounterDecorator::getCompletedCount).tags(tags)
                .description("The approximate total number of tasks that have completed execution").baseUnit("tasks").register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + "executor.active", taskCounterDecorator, TaskCounterDecorator::getActiveCount).tags(tags)
                .description("The approximate number of threads that are actively executing tasks").baseUnit("threads").register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + "executor.queued", taskCounterDecorator, TaskCounterDecorator::getQueuedCount).tags(tags)
                .description("The approximate number of tasks that are queued for execution").baseUnit("tasks").register(meterRegistry);
    }

    public void monitorForkJoinPool(ForkJoinPool fj, String executorServiceName) {
        List<Tag> tags = List.of(Tag.of("name", executorServiceName));
        FunctionCounter.builder(METRIC_PREFIX + "executor.steals", fj, ForkJoinPool::getStealCount)
                .tags(tags)
                .description("Estimate of the total number of tasks stolen from one thread's work queue by another. " +
                        "The reported value underestimates the actual total number of steals when the pool is not quiescent")
                .baseUnit("tasks")
                .register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + "executor.queued", fj, ForkJoinPool::getQueuedTaskCount)
                .tags(tags)
                .description("An estimate of the total number of tasks currently held in queues by worker threads")
                .baseUnit("tasks")
                .register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + "executor.active", fj, ForkJoinPool::getActiveThreadCount)
                .tags(tags)
                .description("An estimate of the number of threads that are currently stealing or executing tasks")
                .baseUnit("threads")
                .register(meterRegistry);
        Gauge.builder(METRIC_PREFIX + "executor.running", fj, ForkJoinPool::getRunningThreadCount)
                .tags(tags)
                .description(
                        "An estimate of the number of worker threads that are not blocked waiting to join tasks or for other managed synchronization threads")
                .baseUnit("threads")
                .register(meterRegistry);
    }

    private ExecutorService decorate(ExecutorService executorService, List<Function<Callable, Callable>> decorators) {
        if (CollectionUtils.isEmpty(decorators)) {
            return executorService;
        } else {
            return new DecoratorExecutorService(executorService, decorators);
        }
    }

    private ExecutorService newVirtualThreadExecutorService(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(namePrefix + "-", 0).factory());
    }

    private ExecutorService monitor(ExecutorService executorService, String executorServiceName) {
        return ExecutorServiceMetrics.monitor(meterRegistry, executorService, executorServiceName, METRIC_PREFIX);
    }

    private ExecutorService monitorVirtual(ExecutorService executorService, TaskCounterDecorator taskCounterDecorator, String executorServiceName) {
        monitorTaskCount(taskCounterDecorator, executorServiceName);
        return ExecutorServiceMetrics.monitor(meterRegistry, executorService, executorServiceName, METRIC_PREFIX);
    }
}
