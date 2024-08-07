package com.sequenceiq.flow.reactor.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventBusStatisticReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusStatisticReporter.class);

    @Inject
    @Named("eventBusThreadPoolExecutor")
    private ExecutorService executor;

    public void logInfoReport() {
        if (executor instanceof ThreadPoolExecutor) {
            LOGGER.info("Reactor event bus statistics: {}", create((ThreadPoolExecutor) executor));
        }
    }

    public void logErrorReport() {
        if (executor instanceof ThreadPoolExecutor) {
            LOGGER.error("Reactor state is critical, statistics: {}", create((ThreadPoolExecutor) executor));
        }
    }

    private EventBusStatistics create(ThreadPoolExecutor executor) {
        EventBusStatistics stats = new EventBusStatistics();

        stats.setPoolSize(executor.getPoolSize());
        stats.setCorePoolSize(executor.getCorePoolSize());
        stats.setTaskCount(executor.getTaskCount());
        stats.setActiveCount(executor.getActiveCount());
        stats.setCompletedTaskCount(executor.getCompletedTaskCount());
        stats.setRemainingCapacity(executor.getQueue().remainingCapacity());

        return stats;
    }

    public static class EventBusStatistics {

        private long taskCount;

        private long completedTaskCount;

        private int corePoolSize;

        private int poolSize;

        private int activeCount;

        private int remainingCapacity;

        public void setTaskCount(long taskCount) {
            this.taskCount = taskCount;
        }

        public void setCompletedTaskCount(long completedTaskCount) {
            this.completedTaskCount = completedTaskCount;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public void setActiveCount(int activeCount) {
            this.activeCount = activeCount;
        }

        public void setRemainingCapacity(int remainingCapacity) {
            this.remainingCapacity = remainingCapacity;
        }

        @Override
        public String toString() {
            return "EventBusStatistics{" +
                    ", taskCount=" + taskCount +
                    ", completedTaskCount=" + completedTaskCount +
                    ", corePoolSize=" + corePoolSize +
                    ", poolSize=" + poolSize +
                    ", activeCount=" + activeCount +
                    ", remainingCapacity=" + remainingCapacity +
                    '}';
        }
    }
}
