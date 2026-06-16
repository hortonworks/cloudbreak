package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;

public class MDCCleanerThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        return new MDCCleanerScheduledExecutor(poolSize, threadFactory, rejectedExecutionHandler);
    }
}
