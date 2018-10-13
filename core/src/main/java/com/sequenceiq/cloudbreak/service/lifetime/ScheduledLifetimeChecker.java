package com.sequenceiq.cloudbreak.service.lifetime;

import java.time.Duration;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesValidationException;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ScheduledLifetimeChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledLifetimeChecker.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
    public void validate() {
        for (Stack stack : stackService.getAllAlive()) {
            getStackTimeToLive(stack).ifPresent(ttl -> {
                try {
                    if (Status.DELETE_IN_PROGRESS != stack.getStatus() && stack.getCluster() != null && stack.getCluster().getCreationFinished() != null) {
                        validateClusterTimeToLive(stack.getCluster().getCreationFinished(), ttl.toMillis());
                    }
                } catch (AccountPreferencesValidationException ignored) {
                    terminateStack(stack);
                }
            });
        }
    }

    private Optional<Duration> getStackTimeToLive(Stack stack) {
        Map<String, String> params = stack.getParameters();
        return Optional.ofNullable(params.get(PlatformParametersConsts.TTL)).map(s -> Duration.ofMillis(Long.parseLong(s)));
    }

    private void terminateStack(Stack stack) {
        if (!stack.isDeleteCompleted()) {
            LOGGER.info("Trigger termination of stack: '{}', workspace: '{}', tenant: '{}'.",
                    stack.getName(), stack.getWorkspace().getName(), stack.getWorkspace().getTenant().getName());
            flowManager.triggerTermination(stack.getId(), false, false);
        }
    }

    private void validateClusterTimeToLive(Long created, Long clusterTimeToLive) throws AccountPreferencesValidationException {
        long now = Calendar.getInstance().getTimeInMillis();
        long clusterRunningTime = now - created;
        if (clusterRunningTime > clusterTimeToLive) {
            LOGGER.info("The maximum running time exceeded by the cluster! clusterRunningTime: {}, clusterTimeToLive: {}",
                    clusterRunningTime, clusterTimeToLive);
            throw new AccountPreferencesValidationException("The maximum running time exceeded by the cluster!");
        }
    }
}
