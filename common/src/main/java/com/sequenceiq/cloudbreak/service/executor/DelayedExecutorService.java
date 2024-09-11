package com.sequenceiq.cloudbreak.service.executor;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
@ConditionalOnBean(name = DELAYED_TASK_EXECUTOR)
public class DelayedExecutorService {

    public static final String DELAYED_TASK_EXECUTOR = "delayedTaskExecutor";

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedExecutorService.class);

    @Inject
    @Qualifier(DELAYED_TASK_EXECUTOR)
    private ScheduledExecutorService delayedTaskExecutor;

    public <T> T runWithDelay(Callable<T> callable, long delay, TimeUnit timeUnit) throws ExecutionException, InterruptedException {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        LOGGER.debug("Scheduling Callable task with {} {}", delay, timeUnit);
        ScheduledFuture<T> scheduledFuture = delayedTaskExecutor.schedule(() -> {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            T result = callable.call();
            MDCBuilder.cleanupMdc();
            return result;
        }, delay, timeUnit);
        LOGGER.debug("Task scheduled, waiting for result");
        T result = scheduledFuture.get();
        LOGGER.debug("Task result is available");
        return result;
    }

    public void runWithDelay(Runnable runnable, long delay, TimeUnit timeUnit) throws ExecutionException, InterruptedException {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        LOGGER.debug("Scheduling Runnable task with {} {}", delay, timeUnit);
        ScheduledFuture<?> scheduledFuture = delayedTaskExecutor.schedule(() -> {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            runnable.run();
            MDCBuilder.cleanupMdc();
        }, delay, timeUnit);
        LOGGER.debug("Task scheduled, waiting to finish");
        scheduledFuture.get();
        LOGGER.debug("Task finished");
    }

    public <T> void runWithDelayWithoutWaitingResult(Runnable runnable, long delay, TimeUnit timeUnit) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        LOGGER.debug("Scheduling Runnable task with {} {}.", delay, timeUnit);
        delayedTaskExecutor.schedule(() -> {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            runnable.run();
            MDCBuilder.cleanupMdc();
        }, delay, timeUnit);
    }
}
