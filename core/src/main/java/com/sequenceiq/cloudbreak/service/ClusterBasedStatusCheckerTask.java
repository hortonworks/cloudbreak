package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public abstract class ClusterBasedStatusCheckerTask<T extends StackContext> extends SimpleStatusCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBasedStatusCheckerTask.class);

    public boolean exitPolling(T t) {
        try {
            Long stackId = t.getStack().getId();
            Long clusterId = t.getStack().getCluster().getId();
            PollGroup stackPollGroup = InMemoryStateStore.getStack(stackId);
            if (stackPollGroup == null || CANCELLED.equals(stackPollGroup)) {
                LOGGER.warn("Stack is getting terminated, polling is cancelled.");
                return true;
            }

            PollGroup clusterPollGroup = InMemoryStateStore.getCluster(clusterId);
            if (clusterPollGroup == null || CANCELLED.equals(clusterPollGroup)) {
                LOGGER.warn("Cluster is getting terminated, polling is cancelled.");
                return true;
            }

            return false;
        } catch (Exception ex) {
            LOGGER.error("Error occurred when check status checker exit criteria: ", ex);
            return true;
        }
    }
}
