package com.sequenceiq.cloudbreak.core.flow2.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class ClusterPollingCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPollingCheckerService.class);

    @Nullable
    public AttemptResult<Object> checkClusterCancelledState(Cluster cluster, boolean cancellable) {
        checkArgument(cluster != null, "Cluster must not be null!");
        AttemptResult<Object> result = null;
        if (cancellable && PollGroup.CANCELLED.equals(InMemoryStateStore.getCluster(cluster.getId()))) {
            LOGGER.info("Cluster wait polling cancelled in inmemory store, id: {}", cluster.getId());
            result = AttemptResults.breakFor("Cluster wait polling cancelled in inmemory store, id: " + cluster.getId());
        }
        return result;
    }

}
