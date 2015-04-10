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

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@Component
public class RetryingStackUpdater {

    private static final int MAX_RETRIES = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingStackUpdater.class);

    @Autowired private StackRepository stackRepository;
    @Autowired private CloudbreakEventService cloudbreakEventService;

    public Stack updateStackStatus(Long stackId, Status status, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateStackStatus(stackId, status, statusReason);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateStackStatus(stackId, status, statusReason);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update status)", stackId), e);
            }
        }
    }

    public Stack updateStackStatusReason(Long stackId, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateStackStatusReason(stackId, statusReason);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateStackStatusReason(stackId, statusReason);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update status)", stackId), e);
            }
        }
    }

    public Stack updateStackMetaData(Long stackId, Set<InstanceMetaData> instanceMetaData, String groupName) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateMetaData(stackId, instanceMetaData, groupName);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack status. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateMetaData(stackId, instanceMetaData, groupName);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update metadata)", stackId), e);
            }
        }
    }

    public Stack updateStackResources(Long stackId, Set<Resource> resources) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateResources(stackId, resources);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateResources(stackId, resources);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update resources)", stackId), e);
            }
        }
    }

    public synchronized Stack addStackResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doAddResources(stackId, resources);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doAddResources(stackId, resources);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update resources)", stackId), e);
            }
        }
    }

    public synchronized Stack removeStackResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doRemoveResources(stackId, resources);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack resources. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doRemoveResources(stackId, resources);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update resources)", stackId), e);
            }
        }
    }

    public Stack updateAmbariIp(Long stackId, String ambariIp) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateAmbariIp(stackId, ambariIp);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack's Ambari IP. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateAmbariIp(stackId, ambariIp);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update ambariIp)", stackId), e);
            }
        }
    }

    public Stack updateStackCluster(Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateStackCluster(stackId, cluster);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while creating corresponding cluster. [attempt: '{}', Cause: {}]. Trying to save it again.",
                        attempt++, e.getClass().getSimpleName());
                return doUpdateStackCluster(stackId, cluster);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to add cluster)", stackId), e);
            }
        }
    }

    public Stack updateMetadataReady(Long stackId, boolean ready) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateMetadataReady(stackId, ready);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while trying to set 'metadataReady'. [attempt: '{}', Cause: {}]. Trying to save it again.",
                        attempt++, e.getClass().getSimpleName());
                return doUpdateMetadataReady(stackId, ready);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts.(while trying to set 'metadataReady')", stackId), e);
            }
        }
    }

    public Stack updateNodeCount(Long stackId, Integer nodeCount, String instanceGroup) {
        Stack stack = stackRepository.findById(stackId);
        int attempt = 1;
        try {
            return doUpdateNodeCount(stackId, nodeCount, instanceGroup);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while trying to set 'nodecount'. [attempt: '{}', Cause: {}]. Trying to save it again.",
                        attempt++, e.getClass().getSimpleName());
                return stack;
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to set 'nodecount')", stackId), e);
            }
        }
    }

    public Stack updateStack(Stack stack) {
        int attempt = 1;
        try {
            return stackRepository.save(stack);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack. [attempt: '{}', Cause: {}]. Trying to save it again.", attempt++, e.getClass().getSimpleName());
                return stackRepository.save(stack);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts.", stack.getId()), e);
            }
        }
    }

    private Stack doUpdateStackStatus(Long stackId, Status status, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        if (!stack.getStatus().equals(Status.DELETE_COMPLETED)) {
            if (status != null) {
                stack.setStatus(status);
            }
            if (statusReason != null) {
                stack.setStatusReason(statusReason);
            }
            stack = stackRepository.save(stack);
            LOGGER.info("Updated stack: [status: '{}', statusReason: '{}'].", status.name(), statusReason);
            cloudbreakEventService.fireCloudbreakEvent(stackId, status.name(), statusReason);
        }
        return stack;
    }

    private Stack doUpdateStackStatusReason(Long stackId, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        if (statusReason != null) {
            stack.setStatusReason(statusReason);
        }
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [statusReason: '{}'].", statusReason);
        return stack;
    }

    private Stack doUpdateMetaData(Long stackId, Set<InstanceMetaData> instanceMetaData, String groupName) {
        Stack stack = stackRepository.findById(stackId);
        stack.getInstanceGroupByInstanceGroupName(groupName).setInstanceMetaData(instanceMetaData);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack metadata.");
        return stack;
    }

    private Stack doUpdateAmbariIp(Long stackId, String ambariIp) {
        Stack stack = stackRepository.findById(stackId);
        stack.setAmbariIp(ambariIp);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [ambariIp: '{}'].", ambariIp);
        return stack;
    }

    private Stack doUpdateNodeCount(Long stackId, Integer nodeCount, String instanceGroup) {
        Stack stack = stackRepository.findById(stackId);
        stack.getInstanceGroupByInstanceGroupName(instanceGroup).setNodeCount(nodeCount);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [nodeCount: '{}'].", nodeCount);
        return stack;
    }

    private Stack doUpdateStackCluster(Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findById(stackId);
        stack.setCluster(cluster);
        stack = stackRepository.save(stack);
        LOGGER.info("Saved cluster '{}' for stack.", cluster.getId());
        return stack;
    }

    private Stack doUpdateMetadataReady(Long stackId, boolean ready) {
        Stack stack = stackRepository.findById(stackId);
        stack.setMetadataReady(ready);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [metadataReady: 'true'].");
        return stack;
    }

    private Stack doUpdateResources(Long stackId, Set<Resource> resources) {
        Stack stack = stackRepository.findById(stackId);
        stack.setResources(resources);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack resources.");
        return stack;
    }

    private Stack doAddResources(Long stackId, List<Resource> resources) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        stack.getResources().addAll(resources);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack resources.");
        return stack;
    }

    private Stack doRemoveResources(Long stackId, List<Resource> resources) {
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
    }

}
