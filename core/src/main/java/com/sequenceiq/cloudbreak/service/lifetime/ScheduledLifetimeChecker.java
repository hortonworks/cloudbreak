package com.sequenceiq.cloudbreak.service.lifetime;

import java.util.Calendar;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationException;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.startup.WorkspaceMigrationRunner;

@Service
public class ScheduledLifetimeChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledLifetimeChecker.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private WorkspaceMigrationRunner workspaceMigrationRunner;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
    public void validate() {
        if (workspaceMigrationRunner.isFinished()) {
            for (StackTtlView stack : stackService.getAllAlive()) {
                stackService.getTtlValueForStack(stack.getId()).ifPresent(ttl -> {
                    try {
                        if (Status.DELETE_IN_PROGRESS != stack.getStatus().getStatus() && stack.getCreationFinished() != null) {
                            validateClusterTimeToLive(stack.getCreationFinished(), ttl.toMillis());
                        }
                    } catch (AccountPreferencesValidationException ignored) {
                        terminateStack(stack);
                    }
                });
            }
        }
    }

    private void terminateStack(StackTtlView stack) {
        if (!Status.DELETE_COMPLETED.equals(stack.getStatus().getStatus())) {
            LOGGER.info("Trigger termination of stack: '{}', owner: '{}', account: '{}'.", stack.getName(), stack.getOwner(), stack.getAccount());
            flowManager.triggerTermination(stack.getId(), false, false);
        }
    }

    private void validateClusterTimeToLive(Long created, Long clusterTimeToLive) throws AccountPreferencesValidationException {
        long now = Calendar.getInstance().getTimeInMillis();
        long clusterRunningTime = now - created;
        if (clusterRunningTime > clusterTimeToLive) {
            throw new AccountPreferencesValidationException("The maximum running time that is configured for the account is exceeded by the cluster!");
        }
    }
}
