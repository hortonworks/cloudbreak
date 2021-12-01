package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ClusterStatusSyncHandler implements ApplicationListener<ClusterStatusSyncEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusSyncHandler.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Override
    public void onApplicationEvent(ClusterStatusSyncEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        LoggingUtils.buildMdcContext(cluster);

        StackStatusV4Response statusResponse = cloudbreakCommunicator.getStackStatusByCrn(cluster.getStackCrn());

        // TODO CB-14929: cluster availability checks needs to be based on the scaling mechanism, and the eventual status of
        // a cluster which has STOPPED instances.
//        boolean clusterAvailable = Optional.ofNullable(statusResponse.getStatus()).map(Status::isAvailable).orElse(false)
//                && Optional.ofNullable(statusResponse.getClusterStatus()).map(Status::isAvailable).orElse(false);

        boolean clusterAvailable = Optional.ofNullable(statusResponse.getStatus()).map(Status::isAvailable).orElse(false);
        boolean clusterNodesUnhealthy = Optional.ofNullable(statusResponse.getStatus()).map(s -> s == Status.NODE_FAILURE).orElse(false);
        LOGGER.info("ZZZ: Computed clusterAvailable: {}, clusterNodesUnhealthy: {}", clusterAvailable, clusterNodesUnhealthy);
        clusterAvailable |= clusterNodesUnhealthy;


        LOGGER.debug("Analysing CBCluster Status '{}' for Cluster '{}. Available(Determined)={}' ", statusResponse, cluster.getStackCrn(), clusterAvailable);
        LOGGER.info("ZZZ: Analysing CBCluster Status '{}' for Cluster '{}. Available(Determined)={}' ",
                statusResponse, cluster.getStackCrn(), clusterAvailable);

        if (DELETE_COMPLETED.equals(statusResponse.getStatus())) {
            clusterService.removeById(autoscaleClusterId);
            LOGGER.info("Deleted cluster '{}', CB Stack Status '{}'.", cluster.getStackCrn(), statusResponse.getStatus());
        } else if (clusterAvailable && !RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), ClusterState.RUNNING);
            LOGGER.info("Updated cluster '{}' to Running, CB Stack Status '{}', CB Cluster Status '{}'.",
                    cluster.getStackCrn(), statusResponse.getStatus(), statusResponse.getClusterStatus());
        } else if (!clusterAvailable && RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), ClusterState.SUSPENDED);
            LOGGER.info("Suspended cluster '{}', CB Stack Status '{}', CB Cluster Status '{}'",
                    cluster.getStackCrn(), statusResponse.getStatus(), statusResponse.getClusterStatus());
        }
    }
}
