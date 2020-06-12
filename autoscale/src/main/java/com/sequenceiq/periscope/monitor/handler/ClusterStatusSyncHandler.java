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
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.ClusterStatusSyncEvent;
import com.sequenceiq.periscope.service.ClusterService;

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
        MDCBuilder.buildMdcContext(cluster);

        Status cbClusterStatus = Optional.ofNullable(cloudbreakCommunicator
                .getStackStatusByCrn(cluster.getStackCrn()).getClusterStatus()).orElse(Status.AMBIGUOUS);
        LOGGER.debug("Analysing CBCluster Status '{}' for Cluster '{}' ", cbClusterStatus, cluster.getStackCrn());

        if (DELETE_COMPLETED.equals(cbClusterStatus)) {
            clusterService.removeById(autoscaleClusterId);
            LOGGER.debug("Deleted cluster '{}', CB Cluster status '{}'.", cluster.getStackCrn(), cbClusterStatus);
        } else if (cbClusterStatus.isAvailable() && !RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), ClusterState.RUNNING);
            LOGGER.debug("Updated cluster '{}' to running, CB Cluster status '{}'.", cluster.getStackCrn(), cbClusterStatus);
        } else if (!cbClusterStatus.isAvailable() && RUNNING.equals(cluster.getState())) {
            clusterService.setState(cluster.getId(), ClusterState.SUSPENDED);
            LOGGER.debug("Suspended cluster '{}', CB Cluster status '{}'", cluster.getStackCrn(), cbClusterStatus);
        }
    }
}
