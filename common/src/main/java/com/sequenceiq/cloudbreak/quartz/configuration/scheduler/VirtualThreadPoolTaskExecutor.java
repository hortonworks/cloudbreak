package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;

public class VirtualThreadPoolTaskExecutor implements ThreadPool, AsyncTaskExecutor, SchedulingTaskExecutor, InitializingBean, DisposableBean {

    private static final long WAIT_FOR_AVAILABLE_THREAD_COUNT = 500;

    private final int poolSize;

    private final Semaphore semaphore;

    private final AtomicBoolean shutdown;

    private final AtomicReference<String> instanceId;

    private final AtomicReference<String> instanceName;

    private final AtomicLong startedTasks;

    private final ExecutorService executor;

    private final boolean waitForJobsToCompleteOnShutdown;

    public VirtualThreadPoolTaskExecutor(String threadName, int poolSize, boolean waitForJobsToCompleteOnShutdown) {
        this.poolSize = poolSize;
        this.semaphore = new Semaphore(poolSize, true);
        shutdown = new AtomicBoolean(false);
        instanceId = new AtomicReference<>();
        instanceName = new AtomicReference<>();
        this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
        startedTasks = new AtomicLong(0);
        executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadName + "-", 0).factory());
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        if (null == runnable || shutdown.get()) {
            return false;
        }
        try {
            semaphore.acquire();
            startedTasks.incrementAndGet();
            executor.submit(() -> {
                try {
                    runnable.run();
                } finally {
                    semaphore.release();
                }
            });
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RejectedExecutionException re) {
            semaphore.release();
            throw re;
        }
    }

    @Override
    public int blockForAvailableThreads() {
        int availablePermits = semaphore.availablePermits();
        while (availablePermits < 1 && !shutdown.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(WAIT_FOR_AVAILABLE_THREAD_COUNT);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
            availablePermits = semaphore.availablePermits();
        }
        return availablePermits;
    }

    @Override
    public void initialize() throws SchedulerConfigException {

    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        shutdown.set(true);
        if (waitForJobsToComplete) {
            semaphore.acquireUninterruptibly(poolSize);
        }
    }

    @Override
    public int getPoolSize() {
        return poolSize;
    }

    @Override
    public void setInstanceId(String s) {
        instanceId.set(s);
    }

    @Override
    public void setInstanceName(String s) {
        instanceName.set(s);
    }

    @Override
    public void destroy() throws Exception {
        shutdown(waitForJobsToCompleteOnShutdown);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task, "Runnable must not be null");
        if (!runInThread(task)) {
            throw new SchedulingException("Quartz SimpleThreadPool already shut down");
        }
    }

    public long getStartedTasks() {
        return startedTasks.get();
    }

    public long getBusyThreads() {
        return poolSize - semaphore.availablePermits();
    }
}
