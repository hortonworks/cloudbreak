package com.sequenceiq.cloudbreak.core.bootstrap.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackDeletionBasedExitCriteria implements ExitCriteria {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionBasedExitCriteria.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        StackDeletionBasedExitCriteriaModel model = (StackDeletionBasedExitCriteriaModel) exitCriteriaModel;
        LOGGER.debug("Check isExitNeeded for model: {}", model);
        try {
            Stack stack = stackService.findLazy(model.getStackId());
            LOGGER.debug("Stack fetched: {}, stack: {}", model.getStackId(), stack);
            if (stack == null || stack.isDeleteInProgress() || stack.isDeleteCompleted()) {
                LOGGER.warn("Stack is in deletion phase no need for more polling");
                return true;
            }
            Cluster cluster = stack.getCluster();
            if (cluster != null && (cluster.isDeleteInProgress() || cluster.isDeleteCompleted())) {
                LOGGER.warn("Cluster is in deletion phase no need for more polling");
                return true;
            }
        } catch (Exception ex) {
            LOGGER.warn("Stack or cluster is in deletion phase no need for more polling");
            return true;
        }
        return false;
    }

    @Override
    public String exitMessage() {
        return "Stack or cluster is in deletion phase no need for more polling";
    }
}
