package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;
import javax.persistence.OptimisticLockException;

import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.SecurityConfigService;

@Component
public class StackUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdater.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private StackStatusUpdater stackStatusUpdater;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        Stack stack = stackService.getStackById(stackId);
        return updateStackStatus(stack, detailedStatus, statusReason);
    }

    @Retryable(value = {OptimisticLockException.class, StaleObjectStateException.class}, backoff = @Backoff(value = 1000), maxAttempts = 4)
    public Stack updateStackStatusWithRetry(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        Stack stack = stackService.getStackById(stackId);
        return updateStackStatus(stack, detailedStatus, statusReason);
    }

    public Stack updateStackStatus(Stack stack, DetailedStackStatus detailedStatus, String statusReason) {
        return stackStatusUpdater.update(stack, detailedStatus, statusReason);
    }

    public Stack updateStackSecurityConfig(Stack stack, SecurityConfig securityConfig) {
        securityConfig = securityConfigService.save(securityConfig);
        stack.setSecurityConfig(securityConfig);
        return stackService.save(stack);
    }

    public Stack updateClusterProxyRegisteredFlag(Stack stack, boolean registered) {
        stack.setClusterProxyRegistered(registered);
        return stackService.save(stack);
    }

}
