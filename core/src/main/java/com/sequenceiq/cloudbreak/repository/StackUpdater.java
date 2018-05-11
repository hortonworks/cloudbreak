package com.sequenceiq.cloudbreak.repository;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

@Component
public class StackUpdater {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus) {
        return doUpdateStackStatus(stackId, detailedStatus, "");
    }

    public Stack updateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        return doUpdateStackStatus(stackId, detailedStatus, statusReason);
    }

    public void updateStackSecurityConfig(Stack stack, SecurityConfig securityConfig) {
        securityConfig = securityConfigRepository.save(securityConfig);
        stack.setSecurityConfig(securityConfig);
        stackRepository.save(stack);
    }

    private Stack doUpdateStackStatus(Long stackId, DetailedStackStatus detailedStatus, String statusReason) {
        Stack stack = stackRepository.findOne(stackId);
        Status status = detailedStatus.getStatus();
        if (!stack.isDeleteCompleted()) {
            stack.setStackStatus(new StackStatus(stack, status, statusReason, detailedStatus));
            if (status.isRemovableStatus()) {
                InMemoryStateStore.deleteStack(stackId);
                if (stack.getCluster() != null && stack.getCluster().getStatus().isRemovableStatus()) {
                    InMemoryStateStore.deleteCluster(stack.getCluster().getId());
                }
            } else {
                InMemoryStateStore.putStack(stackId, statusToPollGroupConverter.convert(status));
            }
            stack = stackRepository.save(stack);
        }
        return stack;
    }
}
