package com.sequenceiq.cloudbreak.core.bootstrap.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackDeletionBasedExitCriteria implements ExitCriteria {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeletionBasedExitCriteria.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        StackDeletionBasedExitCriteriaModel model = (StackDeletionBasedExitCriteriaModel) exitCriteriaModel;
        try {
            Stack stack = stackService.findLazy(model.getStackId());
            if (stack == null || stack.getCluster() == null || stack.isDeleteInProgress() || stack.isDeleteCompleted()
                || stack.getCluster().isDeleteInProgress() || stack.getCluster().isDeleteCompleted()) {
                LOGGER.warn("Stack or cluster is in deletion phase no need for more polling");
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
