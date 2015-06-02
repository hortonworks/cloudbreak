package com.sequenceiq.cloudbreak.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Component
public class RetryingStackUpdater {

    private static final int INITIAL_ATTEMPT = 1;
    private static final int MAX_RETRIES = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingStackUpdater.class);

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    public Stack updateStackStatus(Long stackId, Status status) {
        return doUpdateStackStatus(stackId, status, "", INITIAL_ATTEMPT);
    }

    public Stack updateStackStatus(Long stackId, Status status, String statusReason) {
        return doUpdateStackStatus(stackId, status, statusReason, INITIAL_ATTEMPT);
    }

    public Stack updateStackStatusReason(Long stackId, String statusReason) {
        return doUpdateStackStatusReason(stackId, statusReason, INITIAL_ATTEMPT);
    }

    public Stack updateStackMetaData(Long stackId, Set<InstanceMetaData> instanceMetaData, String groupName) {
        return doUpdateMetaData(stackId, instanceMetaData, groupName, INITIAL_ATTEMPT);
    }

    public Stack updateStackResources(Long stackId, Set<Resource> resources) {
        return doUpdateResources(stackId, resources, INITIAL_ATTEMPT);
    }

    public synchronized Stack addStackResources(Long stackId, List<Resource> resources) {
        return doAddResources(stackId, resources, INITIAL_ATTEMPT);
    }

    public synchronized Stack removeStackResources(Long stackId, List<Resource> resources) {
        return doRemoveResources(stackId, resources, INITIAL_ATTEMPT);
    }

    public Stack updateNodeCount(Long stackId, Integer nodeCount, String instanceGroup) {
        return doUpdateNodeCount(stackId, nodeCount, instanceGroup, INITIAL_ATTEMPT);
    }

    public Stack updateStack(Stack stack) {
        return doUpdateStack(stack, INITIAL_ATTEMPT);
    }

    private Stack doUpdateStack(Stack stack, int attempt) {
        try {
            return stackRepository.save(stack);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doUpdateStack(stack, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts.", stack.getId(), MAX_RETRIES), e);
            }
        }
    }

    private Stack doUpdateStackStatus(Long stackId, Status status, String statusReason, int attempt) {
        try {
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
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doUpdateStackStatus(stackId, status, statusReason, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update status)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doUpdateStackStatusReason(Long stackId, String statusReason, int attempt) {
        try {
            Stack stack = stackRepository.findById(stackId);
            if (statusReason != null) {
                stack.setStatusReason(statusReason);
            }
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack: [statusReason: '{}'].", statusReason);
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doUpdateStackStatusReason(stackId, statusReason, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update status)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doUpdateMetaData(Long stackId, Set<InstanceMetaData> instanceMetaData, String groupName, int attempt) {
        try {
            Stack stack = stackRepository.findById(stackId);
            stack.getInstanceGroupByInstanceGroupName(groupName).setInstanceMetaData(instanceMetaData);
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack metadata.");
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doUpdateMetaData(stackId, instanceMetaData, groupName, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update metadata)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doUpdateNodeCount(Long stackId, Integer nodeCount, String instanceGroup, int attempt) {
        try {
            Stack stack = stackRepository.findById(stackId);
            stack.getInstanceGroupByInstanceGroupName(instanceGroup).setNodeCount(nodeCount);
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack: [nodeCount: '{}'].", nodeCount);
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while trying to set 'nodecount'. [attempt: '{}', Cause: {}]. Trying to save it again.",
                        attempt, e.getClass().getSimpleName());
                return doUpdateNodeCount(stackId, nodeCount, instanceGroup, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to set 'nodecount')",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doUpdateResources(Long stackId, Set<Resource> resources, int attempt) {
        try {
            Stack stack = stackRepository.findById(stackId);
            stack.setResources(resources);
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack resources.");
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doUpdateResources(stackId, resources, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update resources)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doAddResources(Long stackId, List<Resource> resources, int attempt) {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            stack.getResources().addAll(resources);
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack resources.");
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doAddResources(stackId, resources, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update resources)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

    private Stack doRemoveResources(Long stackId, List<Resource> resources, int attempt) {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            Set<Resource> notRemovedResources = new HashSet<>();
            for (Resource resource : stack.getResources()) {
                boolean removable = false;
                for (Resource searchTarget : resources) {
                    if (searchTarget.getResourceName().equals(resource.getResourceName()) && searchTarget.getResourceType().equals(resource.getResourceType())) {
                        removable = true;
                        break;
                    }
                }
                if (!removable) {
                    notRemovedResources.add(resource);
                }
            }

            stack.setResources(notRemovedResources);
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack resources.");
            return stack;
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt, e.getClass().getSimpleName());
                return doRemoveResources(stackId, resources, attempt + 1);
            } else {
                throw new CloudbreakServiceException(String.format("Failed to update stack '%s' in %d attempts. (while trying to update resources)",
                        stackId, MAX_RETRIES), e);
            }
        }
    }

}
