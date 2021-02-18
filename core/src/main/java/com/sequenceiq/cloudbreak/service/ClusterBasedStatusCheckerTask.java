package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;

public abstract class ClusterBasedStatusCheckerTask<T extends StackContext> extends SimpleStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterBasedStatusCheckerTask.class);

    @Override
    public boolean exitPolling(T t) {
        try {
            Long stackId = t.getStack().getId();
            Long clusterId = t.getStack().getCluster().getId();
            PollGroup stackPollGroup = InMemoryStateStore.getStack(stackId);
            if (stackPollGroup == null) {
                LOGGER.warn("No stack poll group found for stack with id '{}'. Stack is getting terminated, polling is cancelled.", stackId);
                return true;
            } else if (CANCELLED.equals(stackPollGroup)) {
                LOGGER.warn("Stack poll group state is 'CANCELED'. Stack is getting terminated, polling is cancelled.");
                return true;
            }

            PollGroup clusterPollGroup = InMemoryStateStore.getCluster(clusterId);
            if (clusterPollGroup == null) {
                LOGGER.warn("No cluster poll group found for cluster with id '{}'. Cluster is getting terminated, polling is cancelled.", clusterId);
                return true;
            } else if (CANCELLED.equals(clusterPollGroup)) {
                LOGGER.warn("Cluster poll group state is 'CANCELED'. Cluster is getting terminated, polling is cancelled.");
                return true;
            }

            return false;
        } catch (Exception ex) {
            LOGGER.error("Error occurred when check status checker exit criteria: ", ex);
            return true;
        }
    }
}
