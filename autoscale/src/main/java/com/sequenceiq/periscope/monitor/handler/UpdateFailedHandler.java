package com.sequenceiq.periscope.monitor.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final String DELETE_STATUS = "DELETE_COMPLETED";

    private static final String STOPPED_STATUSES_PREFIX = "STOP";

    private static final String AVAILABLE = "AVAILABLE";

    private static final int RETRY_THRESHOLD = 5;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackResponseUtils stackResponseUtils;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    private final Map<Long, Integer> updateFailures = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(UpdateFailedEvent event) {
        long autoscaleClusterId = event.getClusterId();
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.debug("Analysing Cluster Status '{}' ", cluster.getStackCrn());
        StackV4Response stackResponse = getStackById(cluster.getStackCrn());
        String stackStatus = getStackStatus(stackResponse);
        if (stackResponse == null || stackStatus.startsWith(STOPPED_STATUSES_PREFIX)) {
            LOGGER.debug("Suspending cluster '{}', Cloudbreak stack status '{}'", cluster.getStackCrn(), stackStatus);
            suspendCluster(cluster);
            return;
        }
        if (stackStatus.equalsIgnoreCase(DELETE_STATUS)) {
            clusterService.removeById(autoscaleClusterId);
            LOGGER.debug("Delete cluster '{}', Cloudbreak stack status '{}'.", cluster.getStackCrn(), stackStatus);
            return;
        }
        Integer failed = updateFailures.get(autoscaleClusterId);
        if (failed == null) {
            LOGGER.debug("New failed cluster : [{}]", cluster.getStackCrn());
            updateFailures.put(autoscaleClusterId, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            Status clusterStatus = stackResponse.getCluster().getStatus();
            if (stackStatus.equals(AVAILABLE) && clusterStatus != null && clusterStatus.name().equals(AVAILABLE)) {
                // Cluster manager server is unreacheable but the stack and cluster statuses are "AVAILABLE"
                LOGGER.debug("Suspend cluster monitoring for cluster '{}' due to failing update attempts and Cloudbreak stack status '{}'",
                        cluster.getStackCrn(), stackStatus);
            } else {
                LOGGER.debug("Suspend cluster monitoring for cluster '{}'", cluster.getStackCrn());
            }
            suspendCluster(cluster);
            updateFailures.remove(autoscaleClusterId);
        } else {
            int value = failed + 1;
            LOGGER.debug("Increase failed count[{}] for cluster id: [{}]", value, cluster.getStackCrn());
            updateFailures.put(autoscaleClusterId, value);
        }
    }

    private String getStackStatus(StackV4Response stackResponse) {
        return stackResponse != null && stackResponse.getStatus() != null ? stackResponse.getStatus().name() : "";
    }

    private StackV4Response getStackById(String stackCrn) {
        try {
            return cloudbreakCommunicator.getByCrn(stackCrn);
        } catch (Exception e) {
            LOGGER.warn("Cluster status could not be verified by Cloudbreak. Original message: {}",
                    e.getMessage());
            return null;
        }
    }

    private void suspendCluster(Cluster cluster) {
        if (!cluster.getState().equals(ClusterState.SUSPENDED)) {
            clusterService.setState(cluster, ClusterState.SUSPENDED);
        }
    }
}
