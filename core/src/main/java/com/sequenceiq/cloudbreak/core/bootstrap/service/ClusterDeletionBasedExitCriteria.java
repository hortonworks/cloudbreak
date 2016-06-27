package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class ClusterDeletionBasedExitCriteria implements ExitCriteria {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDeletionBasedExitCriteria.class);

    @Override
    public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
        ClusterDeletionBasedExitCriteriaModel model = (ClusterDeletionBasedExitCriteriaModel) exitCriteriaModel;
        LOGGER.debug("Check isExitNeeded for model: {}", model);

        PollGroup stackPollGroup = InMemoryStateStore.getStack(model.getStackId());
        if (stackPollGroup == null || CANCELLED.equals(stackPollGroup)) {
            LOGGER.warn("Stack is getting terminated, polling is cancelled.");
            return true;
        }
        if (model.getClusterId() != null) {
            PollGroup clusterPollGroup = InMemoryStateStore.getCluster(model.getClusterId());
            if (clusterPollGroup == null || CANCELLED.equals(clusterPollGroup)) {
                LOGGER.warn("Cluster is getting terminated, polling is cancelled.");
                return true;
            }
        }

        return false;
    }

    @Override
    public String exitMessage() {
        return "Cluster or it's stack is getting terminated, polling is cancelled.";
    }
}
