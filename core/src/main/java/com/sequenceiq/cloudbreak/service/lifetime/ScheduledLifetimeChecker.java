package com.sequenceiq.cloudbreak.service.lifetime;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ScheduledLifetimeChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledLifetimeChecker.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private Clock clock;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
    public void validate() {
        stackService.getAllAlive().forEach(stack ->
                getStackTimeToLive(stack).ifPresent(ttl -> {
                    if (isInDeletableStatus(stack) && isExceeded(stack.getCluster().getCreationFinished(), ttl.toMillis())) {
                        terminateStack(stack);
                    }
                }));
    }

    private boolean isInDeletableStatus(Stack stack) {
        return !stack.isDeleteInProgress() && stack.getCluster() != null && stack.getCluster().getCreationFinished() != null;
    }

    private Optional<Duration> getStackTimeToLive(Stack stack) {
        Map<String, String> params = stack.getParameters();
        if (stack.getParameters() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(params.get(PlatformParametersConsts.TTL_MILLIS))
                .filter(this::isLong)
                .map(s -> Duration.ofMillis(Long.parseLong(s)));
    }

    private boolean isLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            LOGGER.debug("Cannot parse TTL_MILLIS to long: {}", s);
        }
        return false;
    }

    private void terminateStack(Stack stack) {
        if (!stack.isDeleteCompleted()) {
            LOGGER.debug("Trigger termination of stack: '{}', workspace: '{}', tenant: '{}'.",
                    stack.getName(), stack.getWorkspace().getName(), stack.getWorkspace().getTenant().getName());
            flowManager.triggerTermination(stack.getId(), false, false);
        }
    }

    private boolean isExceeded(Long created, Long clusterTimeToLive) {
        long clusterRunningTime = clock.getCurrentTimeMillis() - created;
        if (clusterRunningTime > clusterTimeToLive) {
            LOGGER.info("The maximum running time exceeded by the cluster! clusterRunningTime: {}, clusterTimeToLive: {}",
                    clusterRunningTime, clusterTimeToLive);
            return true;
        }
        return false;
    }
}
