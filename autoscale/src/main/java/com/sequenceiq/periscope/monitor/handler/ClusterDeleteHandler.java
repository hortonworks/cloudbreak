package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.ClusterDeleteEvent;
import com.sequenceiq.periscope.service.AltusMachineUserService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component
public class ClusterDeleteHandler implements ApplicationListener<ClusterDeleteEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDeleteHandler.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    @Override
    public void onApplicationEvent(ClusterDeleteEvent event) {
        Cluster cluster = clusterService.findById(event.getClusterId());
        if (cluster == null) {
            return;
        }
        LoggingUtils.buildMdcContext(cluster);

        StackStatusV4Response statusResponse = cloudbreakCommunicator.getStackStatusByCrn(cluster.getStackCrn());

        if (DELETE_COMPLETED.equals(statusResponse.getStatus())) {
            beforeDeleteCleanup(cluster);
            clusterService.removeById(event.getClusterId());
            LOGGER.info("Deleted cluster: {}, CB Stack status: {}", cluster.getStackCrn(), statusResponse.getStatus());
        }
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
