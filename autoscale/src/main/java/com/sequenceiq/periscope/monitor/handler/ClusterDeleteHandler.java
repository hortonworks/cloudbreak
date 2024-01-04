package com.sequenceiq.periscope.monitor.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ClusterDeleteHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDeleteHandler.class);

    private static final long SLEEP_DURATION_FOR_WAIT_DELETE_CLUSTERS = 500;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Inject
    @Qualifier("periscopeDeleteScheduledExecutorService")
    private ExecutorService deleteExecutorService;

    @Value("${periscope.maxDeleteRetryCount:5}")
    private int maxDeleteRetryCount;

    public void deleteClusters(Long since) {
        LOGGER.info("Fetch Clusters deleted since {} from CB", since);
        List<StackStatusV4Response> stacks = cloudbreakCommunicator.getDeletedClusters(since);
        LOGGER.info("Clusters to delete based on response from CB {}", stacks);
        List<Future<String>> clustersToBeDeleted = new ArrayList<>();
        for (StackStatusV4Response stack : stacks) {
            Cluster cluster = clusterService.findOneByStackCrn(stack.getCrn()).orElse(null);
            if (cluster != null) {
                clustersToBeDeleted.add(deleteExecutorService.submit(() -> deleteCluster(cluster)));
            } else {
                LOGGER.info("cluster: {} fetched from CB but does not exist", stack.getCrn());
            }
        }
        List<Cluster> clustersToRetry = clusterService.findByDeleteRetryCount(maxDeleteRetryCount);
        LOGGER.info("number of clusters to retry : {} ", clustersToRetry.size());
        clustersToRetry.forEach(cluster -> {
            clustersToBeDeleted.add(deleteExecutorService.submit(() -> deleteCluster(cluster)));
        });
        waitForClustersToDelete(clustersToBeDeleted);
    }

    private void waitForClustersToDelete(List<Future<String>> clustersToBeDeleted) {
        while (!clustersToBeDeleted.isEmpty()) {
            List<Future<String>> done = new ArrayList<>();
            for (Future<String> future : clustersToBeDeleted) {
                if (future.isDone()) {
                    done.add(future);
                    try {
                        future.get();
                    } catch (Exception e) {
                        LOGGER.error("Error during deleting cluster {}", e.getMessage());
                    }
                }
            }
            clustersToBeDeleted.removeAll(done);
            if (!clustersToBeDeleted.isEmpty()) {
                try {
                    Thread.sleep(SLEEP_DURATION_FOR_WAIT_DELETE_CLUSTERS);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException encountered during sleep");
                }
            }
        }
    }

    public String deleteCluster(Cluster cluster) {
        LoggingUtils.buildMdcContext(cluster);
        try {
            beforeDeleteCleanup(cluster);
            clusterService.removeById(cluster.getId());
            LOGGER.info("Deleted cluster: {}", cluster.getStackCrn());
        } catch (Exception e) {
            LOGGER.info("Deletion failed for cluster: {} with reason: {}", cluster.getStackCrn(), e.getMessage());
            clusterService.updateClusterDeleted(cluster.getId(), ClusterState.DELETED,
                    (cluster.getDeleteRetryCount() == null ? 0 : cluster.getDeleteRetryCount()) + 1);
        }
        return cluster.getStackCrn();
    }

    protected void beforeDeleteCleanup(Cluster cluster) {
        try {
            if (cluster.getEnvironmentCrn() != null && clusterService.countByEnvironmentCrn(cluster.getEnvironmentCrn()) <= 1) {
                altusMachineUserService.deleteMachineUserForEnvironment(cluster.getClusterPertain().getTenant(),
                        cluster.getMachineUserCrn(), cluster.getEnvironmentCrn());
            }
        } catch (Exception ex) {
            LOGGER.warn("Error deleting machineUserCrn '{}' for environment '{}'",
                    cluster.getMachineUserCrn(), cluster.getEnvironmentCrn(), ex);
        }
    }
}
