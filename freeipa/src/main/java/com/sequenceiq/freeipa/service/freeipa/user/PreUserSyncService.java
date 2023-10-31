package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.converter.stack.StackToStackUserSyncViewConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class PreUserSyncService extends AbstractUserSyncTaskRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreUserSyncService.class);

    @Inject
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Inject
    private UmsVirtualGroupCreateService umsVirtualGroupCreateService;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_EXTERNAL_TASK_EXECUTOR)
    private ExecutorService usersyncExternalTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private StackToStackUserSyncViewConverter stackUserSyncViewConverter;

    protected void asyncRunTask(String operationId, String accountId, Stack stack) {
        LOGGER.debug("Scheduling pre usersync task with [{}] operation id", operationId);
        Future<?> task = usersyncExternalTaskExecutor.submit(() -> {
            LOGGER.info("Starting pre usersync task with [{}] operation id", operationId);
            umsVirtualGroupCreateService.createVirtualGroups(accountId, List.of(stackUserSyncViewConverter.convert(stack)));
            operationService.completeOperation(accountId, operationId, List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        });
        timeoutTaskScheduler.scheduleTimeoutTask(operationId, accountId, task, getOperationTimeout());
    }
}
