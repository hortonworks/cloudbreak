package com.sequenceiq.cloudbreak.logger.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerScheduledExecutor extends ScheduledThreadPoolExecutor {

    public MDCCleanerScheduledExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public MDCCleanerScheduledExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(poolSize, threadFactory, rejectedExecutionHandler);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        MDCBuilder.cleanupMdc();
    }
}