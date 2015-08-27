package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Component
public class StackUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdater.class);

    @Inject
    private StackRepository stackRepository;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private ResourceRepository resourceRepository;
    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    public Stack updateStackStatus(Long stackId, Status status) {
        return doUpdateStackStatus(stackId, status, "");
    }

    public Stack updateStackStatus(Long stackId, Status status, String statusReason) {
        return doUpdateStackStatus(stackId, status, statusReason);
    }

    public Stack addStackResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findById(stackId);
        for (Resource resource : resources) {
            resource.setStack(stack);
        }
        resourceRepository.save(resources);
        stack.getResources().addAll(resources);
        return stackRepository.save(stack);
    }

    public void removeStackResources(List<Resource> resources) {
        resourceRepository.delete(resources);
    }

    private Stack doUpdateStackStatus(Long stackId, Status status, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        if (!stack.isDeleteCompleted()) {
            if (status != null) {
                stack.setStatus(status);
            }
            if (statusReason != null) {
                stack.setStatusReason(statusReason);
            }
            InMemoryStateStore.put(stackId, statusToPollGroupConverter.convert(status));
            if (Status.DELETE_COMPLETED.equals(status)) {
                InMemoryStateStore.delete(stackId);
            } else {
                stack = stackRepository.save(stack);
            }
        }
        return stack;
    }

}
