package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component
public class TimeoutTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutTaskScheduler.class);

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_TIMEOUT_TASK_EXECUTOR)
    private ScheduledExecutorService timeoutTaskExecutor;

    @Inject
    private OperationService operationService;

    public void scheduleTimeoutTask(String operationId, String accountId, Future<?> task, Long operationTimeout) {
        LOGGER.info("Scheduling timeout task for {} with {}ms timeout", operationId, operationTimeout);
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        timeoutTaskExecutor.schedule(() -> {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            if (task.isCancelled() || task.isDone()) {
                LOGGER.debug("Nothing to do for operation id: [{}]", operationId);
            } else {
                LOGGER.debug("Terminating usersync task with operation id: [{}]", operationId);
                task.cancel(true);
                operationService.timeout(operationId, accountId);
            }
            MDCBuilder.cleanupMdc();
        }, operationTimeout, TimeUnit.MILLISECONDS);
    }
}
