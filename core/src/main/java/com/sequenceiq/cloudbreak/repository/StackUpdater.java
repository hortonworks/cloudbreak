package com.sequenceiq.cloudbreak.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    public Stack updateStackStatus(Long stackId, Status status) {
        return doUpdateStackStatus(stackId, status, "");
    }

    public Stack updateStackStatus(Long stackId, Status status, String statusReason) {
        return doUpdateStackStatus(stackId, status, statusReason);
    }

    public Stack updateStackStatusReason(Long stackId, String statusReason) {
        return doUpdateStackStatusReason(stackId, statusReason);
    }

    public Stack addStackResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        for (Resource resource : resources) {
            resource.setStack(stack);
        }
        resourceRepository.save(resources);
        stack.getResources().addAll(resources);
        return stackRepository.save(stack);
    }

    public Stack removeStackResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Set<Resource> removableResources = new HashSet<>();
        for (Resource resource : stack.getResources()) {
            for (Resource searchTarget : resources) {
                if (searchTarget.getResourceName().equals(resource.getResourceName()) && searchTarget.getResourceType().equals(resource.getResourceType())) {
                    removableResources.add(resource);
                    break;
                }
            }
        }
        resourceRepository.delete(removableResources);
        return stack;
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
            stack = stackRepository.save(stack);
            if (statusReason != null && !statusReason.isEmpty()) {
                LOGGER.info("Updated stack: [status: '{}', statusReason: '{}'].", status.name(), statusReason);
                cloudbreakEventService.fireCloudbreakEvent(stackId, status.name(), statusReason);
            }
        }
        return stack;
    }

    private Stack doUpdateStackStatusReason(Long stackId, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        if (statusReason != null) {
            stack.setStatusReason(statusReason);
            cloudbreakEventService.fireCloudbreakEvent(stackId, stack.getStatus().name(), statusReason);
        }
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [statusReason: '{}'].", statusReason);
        return stack;
    }

}
