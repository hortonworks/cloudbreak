package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.SecurityConfigService;

@Component
public class StackUpdater {

    @Inject
    private StackService stackService;

    @Inject
    private SecurityConfigService securityConfigService;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stackId, detailedStatus, statusReason);
    }

    public Stack updateStackStatus(Stack stack, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stack, detailedStatus, statusReason);
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

    private Stack doUpdateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        Stack stack = stackService.getStackById(stackId);
        return doUpdateStackStatus(stack, detailedStatus, statusReason);
    }

    private Stack doUpdateStackStatus(Stack stack, DetailedStackStatus detailedStatus, String statusReason) {
        Status status = detailedStatus.getStatus();
        if (!Status.DELETE_COMPLETED.equals(stack.getStackStatus().getStatus())) {
            stack.setStackStatus(new StackStatus(stack, status, statusReason, detailedStatus));
            stack = stackService.save(stack);
            if (status.isRemovableStatus()) {
                InMemoryStateStore.deleteStack(stack.getId());
            } else {
                PollGroup pollGroup = Status.DELETE_COMPLETED.equals(status) ? PollGroup.CANCELLED : PollGroup.POLLABLE;
                InMemoryStateStore.putStack(stack.getId(), pollGroup);
            }
        }
        return stack;
    }
}
