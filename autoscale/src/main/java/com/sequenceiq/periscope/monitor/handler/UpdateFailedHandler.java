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

import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.log.MDCBuilder;
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
        long id = event.getClusterId();
        LOGGER.info("Cluster {} failed", id);
        Cluster cluster = clusterService.findById(id);
        if (cluster == null) {
            return;
        }
        MDCBuilder.buildMdcContext(cluster);
        StackResponse stackResponse = getStackById(id);
        if (stackResponse == null) {
            LOGGER.info("Suspending cluster {}", id);
            suspendCluster(cluster);
            return;
        }
        String stackStatus = getStackStatus(stackResponse);
        if (stackStatus.startsWith(DELETE_STATUSES_PREFIX)) {
            clusterService.removeById(id);
            LOGGER.info("Delete cluster {} due to failing update attempts and Cloudbreak stack status", id);
            return;
        }
        Integer failed = updateFailures.get(id);
        if (failed == null) {
            LOGGER.info("New failed cluster id: [{}]", id);
            updateFailures.put(id, 1);
        } else if (RETRY_THRESHOLD - 1 == failed) {
            try {
                String clusterStatus = stackResponse.getCluster().getStatus().name();
                if (stackStatus.equals(AVAILABLE) && clusterStatus.equals(AVAILABLE)) {
                    // Ambari server is unreacheable but the stack and cluster statuses are "AVAILABLE"
                    reportAmbariServerFailure(cluster, stackResponse);
                    suspendCluster(cluster);
                    LOGGER.info("Suspend cluster monitoring for cluster {} due to failing update attempts and Cloudbreak stack status {}", id, stackStatus);
                } else {
                    suspendCluster(cluster);
                    LOGGER.info("Suspend cluster monitoring for cluster {}", id);
                }
            } catch (Exception ex) {
                LOGGER.warn("Problem when verifying cluster status. Original message: {}",
                        ex.getMessage());
                suspendCluster(cluster);
            }
            updateFailures.remove(id);
        } else {
            int value = failed + 1;
            LOGGER.info("Increase failed count[{}] for cluster id: [{}]", value, id);
            updateFailures.put(id, value);
        }
    }

    private String getStackStatus(StackResponse stackResponse) {
        return stackResponse.getStatus() != null ? stackResponse.getStatus().name() : "";
    }

    private StackResponse getStackById(long stackId) {
        try {
            return cloudbreakCommunicator.getById(stackId);
        } catch (Exception e) {
            LOGGER.warn("Cluster status could not be verified by Cloudbreak. Original message: {}",
                    e.getMessage());
            return null;
        }
    }

    private void suspendCluster(Cluster cluster) {
        clusterService.setState(cluster, ClusterState.SUSPENDED);
    }

    private void reportAmbariServerFailure(Cluster cluster, StackResponse stackResponse) {
        Optional<InstanceMetaDataJson> pgw = stackResponseUtils.getNotTerminatedPrimaryGateways(stackResponse);
        if (pgw.isPresent()) {
            FailureReport failureReport = new FailureReport();
            failureReport.setFailedNodes(Collections.singletonList(pgw.get().getDiscoveryFQDN()));
            try {
                cloudbreakCommunicator.failureReport(cluster.getStackId(), failureReport);
            } catch (Exception e) {
                LOGGER.warn("Exception during failure report. Original message: {}", e.getMessage());
            }
        }
    }
}
