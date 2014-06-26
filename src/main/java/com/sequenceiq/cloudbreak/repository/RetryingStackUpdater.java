package com.sequenceiq.cloudbreak.repository;

import javax.persistence.OptimisticLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;

@Component
public class RetryingStackUpdater {

    private static final int MAX_RETRIES = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingStackUpdater.class);

    @Autowired
    private StackRepository stackRepository;

    public Stack updateStackStatus(Long stackId, Status status) {
        return updateStackStatus(stackId, status, null);
    }

    public Stack updateStackStatus(Long stackId, Status status, String statusReason) {
        int attempt = 1;
        try {
            return doUpdateStackStatus(stackId, status, statusReason);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack status. [id: '{}', attempt: '{}', Cause: {}]. Trying to save it again.",
                    stackId, attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateStackStatus(stackId, status, statusReason);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update status)", stackId), e);
            }
        }
    }

    public Stack updateAmbariIp(Long stackId, String ambariIp) {
        int attempt = 1;
        try {
            return doUpdateAmbariIp(stackId, ambariIp);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack's Ambari IP. [id: '{}', attempt: '{}', Cause: {}]. Trying to save it again.",
                    stackId, attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateAmbariIp(stackId, ambariIp);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to update ambariIp)", stackId), e);
            }
        }
    }

    public Stack updateStackCfAttributes(Long stackId, String cfStackName, String cfStackId) {
        int attempt = 1;
        try {
            return doUpdateStackCfAttributes(stackId, cfStackName, cfStackId);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            LOGGER.info("Failed to update stack's CloudFormation attributes. [id: '{}', attempt: '{}', Cause: {}]. Trying to save it again.",
                    stackId, attempt++, e.getClass().getSimpleName());
            if (attempt <= MAX_RETRIES) {
                return doUpdateStackCfAttributes(stackId, cfStackName, cfStackId);
            } else {
                throw new InternalServerException(String.format(
                        "Failed to update stack '%s' in 5 attempts. (while trying to update CloudFormation attributes)", stackId), e);
            }
        }
    }

    public Stack updateStackCluster(Long stackId, Cluster cluster) {
        int attempt = 1;
        try {
            return doUpdateStackCluster(stackId, cluster);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while creating corresponding cluster. [id: '{}', attempt: '{}', Cause: {}]. Trying to save it again.",
                        stackId, attempt++, e.getClass().getSimpleName());
                return doUpdateStackCluster(stackId, cluster);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to add cluster)", stackId), e);
            }
        }
    }

    public Stack updateCfStackCreateComplete(Long stackId) {
        int attempt = 1;
        try {
            return doUpdateCfStackCreateComplete(stackId);
        } catch (OptimisticLockException | OptimisticLockingFailureException e) {
            if (attempt <= MAX_RETRIES) {
                LOGGER.info("Failed to update stack while trying to set 'CF stack completed'. [id: '{}', attempt: '{}', Cause: {}]. Trying to save it again.",
                        stackId, attempt++, e.getClass().getSimpleName());
                return doUpdateCfStackCreateComplete(stackId);
            } else {
                throw new InternalServerException(String.format("Failed to update stack '%s' in 5 attempts. (while trying to set 'CF stack completed')",
                        stackId), e);
            }
        }
    }

    private Stack doUpdateStackStatus(Long stackId, Status status, String statusReason) {
        Stack stack = stackRepository.findById(stackId);
        stack.setStatus(status);
        if (statusReason != null) {
            stack.setStatusReason(statusReason);
        }
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [stack: '{}', status: '{}', statusReason: '{}'].", stackId, status.name(), statusReason);
        return stack;
    }

    private Stack doUpdateAmbariIp(Long stackId, String ambariIp) {
        Stack stack = stackRepository.findById(stackId);
        stack.setAmbariIp(ambariIp);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [stack: '{}' ambariIp: '{}'].", stackId, ambariIp);
        return stack;
    }

    private Stack doUpdateStackCfAttributes(Long stackId, String cfStackName, String cfStackId) {
        Stack stack = stackRepository.findById(stackId);
        stack.setCfStackId(cfStackId);
        stack.setCfStackName(cfStackName);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack '{}' with CloudFormation properties: [name: '{}', id: '{}'].", stackId, cfStackName, cfStackId);
        return stack;
    }

    private Stack doUpdateStackCluster(Long stackId, Cluster cluster) {
        Stack stack = stackRepository.findById(stackId);
        stack.setCluster(cluster);
        stack = stackRepository.save(stack);
        LOGGER.info("Saved cluster '{}' for stack '{}'.", cluster.getId(), stackId);
        return stack;
    }

    private Stack doUpdateCfStackCreateComplete(Long stackId) {
        Stack stack = stackRepository.findById(stackId);
        stack.setCfStackCompleted(true);
        stack = stackRepository.save(stack);
        LOGGER.info("Updated stack: [stack: '{}' cfStackCompleted: 'true'].", stackId);
        return stack;
    }

}
