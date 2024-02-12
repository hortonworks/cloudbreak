package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Custom ThreadPool implementation to configure a custom TimedSimpleThreadPoolTaskExecutor for Quartz and delegate calls from the Quartz scheduler
 * to the underlying thread pool.
 * <p>
 * This class is loaded and initalized during Quartz scheduler creation and responsible to load the related task executor bean from Spring.
 * The original class in Spring was org.springframework.scheduling.quartz.LocalTaskExecutorThreadPool. This class was implemented by Spring
 * in a non-blocking way (blockForAvailableThreads() method always returned 1) which is incompatible with the way Quartz works.
 * This prevented Quartz from properly calculating misfired triggers and resulted in missing alerts.
 * <p>
 * The LocalTimedExecutorThreadPool class delegate calls to the underlying TimedSimpleThreadPoolTaskExecutor which is an enhanced SimpleThreadPool
 * implementation to provide metrics (threadpool size, usage, etc.).
 * <p>
 * To enable, set the following property in quartz.properties and set a custom taskExecutor (TimedSimpleThreadPoolTaskExecutor)
 * for the SchedulerFactoryBeanCustomizer, see: com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.
 * spring.quartz.properties.org.quartz.threadPool.class=com.sequenceiq.cloudbreak.quartz.configuration.scheduler.LocalTimedExecutorThreadPool
 */
public class LocalTimedExecutorThreadPool implements ThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTimedExecutorThreadPool.class);

    private TimedSimpleThreadPoolTaskExecutor timedExecutor;

    private String instanceId;

    private String instanceName;

    public LocalTimedExecutorThreadPool() {
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        Executor taskExecutor = SchedulerFactoryBean.getConfigTimeTaskExecutor();
        if (taskExecutor == null) {
            throw new SchedulerConfigException("No local Executor found for configuration - 'taskExecutor' property must be set on SchedulerFactoryBean");
        }
        if (!(taskExecutor instanceof TimedSimpleThreadPoolTaskExecutor)) {
            throw new SchedulerConfigException("No valid Executor found for configuration - 'taskExecutor'" +
                    " must be a TimedSimpleThreadPoolTaskExecutor instance");
        }
        timedExecutor = (TimedSimpleThreadPoolTaskExecutor) taskExecutor;

        if (instanceId != null) {
            timedExecutor.setInstanceId(instanceId);
        }
        if (instanceName != null) {
            timedExecutor.setInstanceName(instanceName);
        }

        timedExecutor.initialize();
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        try {
            return timedExecutor.runInThread(runnable);
        } catch (RejectedExecutionException exception) {
            LOGGER.error("Task has been rejected by TaskExecutor", exception);
            return false;
        }
    }

    @Override
    public int blockForAvailableThreads() {
        return timedExecutor.blockForAvailableThreads();
    }

    @Override
    public void setInstanceId(String schedInstId) {
        this.instanceId = schedInstId;
    }

    @Override
    public void setInstanceName(String schedName) {
        this.instanceName = schedName;
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        timedExecutor.shutdown(waitForJobsToComplete);
    }

    @Override
    public int getPoolSize() {
        if (timedExecutor != null) {
            return timedExecutor.getPoolSize();
        } else {
            return -1;
        }
    }
}
