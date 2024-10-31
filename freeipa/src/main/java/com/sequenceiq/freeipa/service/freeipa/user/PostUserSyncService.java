package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_SUDO_RULES;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.converter.stack.StackToStackUserSyncViewConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class PostUserSyncService extends AbstractUserSyncTaskRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostUserSyncService.class);

    @Inject
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_EXTERNAL_TASK_EXECUTOR)
    private ExecutorService usersyncExternalTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SudoRuleService sudoRuleService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackToStackUserSyncViewConverter syncViewConverter;

    protected void asyncRunTask(String operationId, String accountId, Stack stack) {
        Future<?> task = usersyncExternalTaskExecutor.submit(() -> {
            LOGGER.debug("Starting {} ...", ADD_SUDO_RULES);
            try {
                FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
                sudoRuleService.setupSudoRule(syncViewConverter.convert(stack), freeIpaClient);
                operationService.completeOperation(accountId, operationId, List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
            } catch (Exception e) {
                LOGGER.error("{} failed for environment '{}'.", ADD_SUDO_RULES, stack.getEnvironmentCrn(), e);
                operationService.failOperation(accountId, operationId, ADD_SUDO_RULES + " failed for environment with " + e.getMessage());
            }
            LOGGER.debug("Finished {}.", ADD_SUDO_RULES);
        });
        timeoutTaskScheduler.scheduleTimeoutTask(operationId, accountId, task, getOperationTimeout());
    }
}
