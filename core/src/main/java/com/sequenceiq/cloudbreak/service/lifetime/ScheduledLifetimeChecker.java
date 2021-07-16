package com.sequenceiq.cloudbreak.service.lifetime;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
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
                stackService.getTtlValueForStack(stack.getId()).ifPresent(ttl -> {
                    if (isInDeletableStatus(stack) && isExceeded(stack.getCreationFinished(), ttl.toMillis())) {
                        LOGGER.info("Stack is exceeded at {}ms because is in deletable status and ttl is expired, ttl: {}",
                                stack.getCreationFinished() + ttl.toMillis(), ttl.toMillis());
                        terminateStack(stack);
                    }
                }));
    }

    private boolean isInDeletableStatus(StackTtlView stack) {
        return Status.DELETE_IN_PROGRESS != stack.getStatus().getStatus() && stack.getCreationFinished() != null;
    }

    private void terminateStack(StackTtlView stack) {
        if (!Status.DELETE_COMPLETED.equals(stack.getStatus().getStatus())) {
            LOGGER.debug("Trigger termination of stack: '{}', workspace: '{}', tenant: '{}'.",
                    stack.getName(), stack.getWorkspace().getName(), stack.getWorkspace().getTenant().getName());
            flowManager.triggerTermination(stack.getId());
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
