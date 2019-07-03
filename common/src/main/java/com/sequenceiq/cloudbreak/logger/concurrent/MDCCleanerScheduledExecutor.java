package com.sequenceiq.cloudbreak.logger.concurrent;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerScheduledExecutor extends ScheduledThreadPoolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCCleanerScheduledExecutor.class);

    private Optional<Consumer<ExecutorService>> metricConsumer = Optional.empty();

    public MDCCleanerScheduledExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public MDCCleanerScheduledExecutor(int corePoolSize, ThreadFactory threadFactory, Consumer<ExecutorService> metricConsumer) {
        super(corePoolSize, threadFactory);
        this.metricConsumer = Optional.of(metricConsumer);
    }

    public MDCCleanerScheduledExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(poolSize, threadFactory, rejectedExecutionHandler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        LOGGER.info("Threadpool tasks: active threads: {}, poolsize: {}, queueSize: {}, completed tasks {}",
                getActiveCount(),
                getPoolSize(),
                getQueue().size(),
                getCompletedTaskCount()
        );
        metricConsumer.ifPresent(metric -> metric.accept(this));
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        MDCBuilder.cleanupMdc();
    }
}