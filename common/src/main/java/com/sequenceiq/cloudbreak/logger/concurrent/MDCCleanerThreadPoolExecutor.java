package com.sequenceiq.cloudbreak.logger.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerThreadPoolExecutor extends ThreadPoolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCCleanerThreadPoolExecutor.class);

    public MDCCleanerThreadPoolExecutor(int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable command) {
        try {
            super.execute(command);
        } catch (Exception e) {
            LOGGER.error("Exception happened in thread pool execute method", e);
            throw e;
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        MDCBuilder.cleanupMdc();
    }
}