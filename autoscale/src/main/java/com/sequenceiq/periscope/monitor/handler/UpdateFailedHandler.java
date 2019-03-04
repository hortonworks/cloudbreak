package com.sequenceiq.periscope.monitor.handler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.FailureReportV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@Component
public class UpdateFailedHandler implements ApplicationListener<UpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFailedHandler.class);

    private static final String DELETE_STATUSES_PREFIX = "DELETE_";

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
        LOGGER.debug("Cluster {} failed", autoscaleClusterId);
        Cluster cluster = clusterService.findById(autoscaleClusterId);
        if (cluster == null) {
            return;
        }
        MDCBuilder.buildMdcContext(cluster);
        StackV4Response stackResponse = getStackById(cluster.getStackId());
        if (stackResponse == null) {
            LOGGER.debug("Suspending cluster {}", autoscaleClusterId);
            suspendCluster(cluster);
            return;
        }
        String stackStatus = getStackStatus(stackResponse);
        if (stackStatus.startsWith(DELETE_STATUSES_PREFIX)) {
            clusterService.removeById(autoscaleClusterId);
            LOGGER.debug("Delete cluster {} due to failing update attempts and Cloudbreak stack status", autoscaleClusterId);
            return;
        }
        Integer failed = updateFailures.get(autoscaleClusterId);
        if (failed == null) {
            LOGGER.debug("New failed cluster id: [{}]", autoscaleClusterId);
            updateFailures.put(autoscaleClusterId, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            try {
                String clusterStatus = stackResponse.getCluster().getStatus().name();
                if (stackStatus.equals(AVAILABLE) && clusterStatus.equals(AVAILABLE)) {
                    // Ambari server is unreacheable but the stack and cluster statuses are "AVAILABLE"
                    reportAmbariServerFailure(cluster, stackResponse);
                    suspendCluster(cluster);
                    LOGGER.debug("Suspend cluster monitoring for cluster {} due to failing update attempts and Cloudbreak stack status {}",
                            autoscaleClusterId, stackStatus);
                } else {
                    suspendCluster(cluster);
                    LOGGER.debug("Suspend cluster monitoring for cluster {}", autoscaleClusterId);
                }
            } catch (Exception ex) {
                LOGGER.warn("Problem when verifying cluster status. Original message: {}",
                        ex.getMessage());
                suspendCluster(cluster);
            }
            updateFailures.remove(autoscaleClusterId);
        } else {
            int value = failed + 1;
            LOGGER.debug("Increase failed count[{}] for cluster id: [{}]", value, autoscaleClusterId);
            updateFailures.put(autoscaleClusterId, value);
        }
    }

    private String getStackStatus(StackV4Response stackResponse) {
        return stackResponse.getStatus() != null ? stackResponse.getStatus().name() : "";
    }

    private StackV4Response getStackById(long cloudbreakStackId) {
        try {
            return cloudbreakCommunicator.getById(cloudbreakStackId);
        } catch (Exception e) {
            LOGGER.warn("Cluster status could not be verified by Cloudbreak. Original message: {}",
                    e.getMessage());
            return null;
        }
    }

    private void suspendCluster(Cluster cluster) {
        clusterService.setState(cluster, ClusterState.SUSPENDED);
    }

    private void reportAmbariServerFailure(Cluster cluster, StackV4Response stackResponse) {
        Optional<InstanceMetaDataV4Response> pgw = stackResponseUtils.getNotTerminatedPrimaryGateways(stackResponse);
        if (pgw.isPresent()) {
            FailureReportV4Request failureReport = new FailureReportV4Request();
            failureReport.setFailedNodes(Collections.singletonList(pgw.get().getDiscoveryFQDN()));
            try {
                cloudbreakCommunicator.failureReport(cluster.getStackId(), failureReport);
            } catch (Exception e) {
                LOGGER.warn("Exception during failure report. Original message: {}", e.getMessage());
            }
        }
    }
}
