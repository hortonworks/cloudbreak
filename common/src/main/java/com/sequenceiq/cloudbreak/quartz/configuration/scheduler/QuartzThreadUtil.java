package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_METERING_SYNC_EXECUTOR_THREAD_NAME_PREFIX;

public class QuartzThreadUtil {

    private QuartzThreadUtil() {
    }

    public static boolean isCurrentQuartzThread() {
        String threadName = Thread.currentThread().getName();
        return threadName.startsWith(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)
                || threadName.startsWith(QUARTZ_METERING_EXECUTOR_THREAD_NAME_PREFIX)
                || threadName.startsWith(QUARTZ_METERING_SYNC_EXECUTOR_THREAD_NAME_PREFIX);
    }
}
