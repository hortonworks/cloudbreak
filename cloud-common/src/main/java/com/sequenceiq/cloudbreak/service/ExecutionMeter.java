package com.sequenceiq.cloudbreak.service;

import java.util.UUID;

import org.slf4j.Logger;

public class ExecutionMeter implements AutoCloseable {
    private final UUID taskId = UUID.randomUUID();

    private final long startTime = System.currentTimeMillis();

    private final String taskName;

    private final Logger logger;

    private ExecutionMeter(String taskName, Logger logger) {
        this.taskName = taskName;
        this.logger = logger;
        logger.debug("Duration metering of task started, taskname: {}, id: {}", taskName, taskId);
    }

    public static ExecutionMeter start(String taskName, Logger logger) {
        return new ExecutionMeter(taskName, logger);
    }

    @Override
    public void close() {
        logger.debug("Duration metering of task finished, taskname: {}, id: {}, duration: {} ms", taskName, taskId, System.currentTimeMillis() - startTime);
    }
}
